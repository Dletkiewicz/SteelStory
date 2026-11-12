package com.steelstory.identity;

import com.steelstory.SteelstoryApplication;
import com.steelstory.identity.api.AccountAccess;
import com.steelstory.identity.api.AccountId;
import com.steelstory.identity.internal.application.AccountRepository;
import com.steelstory.identity.internal.application.IdentityService;
import com.steelstory.identity.internal.application.PasswordHashing;
import com.steelstory.identity.internal.application.RegisterAccountCommand;
import com.steelstory.identity.internal.domain.Account;
import com.steelstory.identity.internal.domain.EmailAddress;
import com.steelstory.identity.internal.domain.PasswordHash;
import java.io.ByteArrayInputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.session.cookie.secure=false")
class IdentityIT {
    private static final String PASSWORD = "synthetic password 😀 ";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            "postgres:18.6@sha256:e3d24f6906d0760bf4da0dae872415391ededefad23fd471c47dc53b0ddf3c42")
            .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AccountAccess accountAccess;
    @Autowired AccountRepository accountRepository;
    @Autowired PasswordHashing passwordHashing;
    @Autowired Clock clock;
    @Autowired PlatformTransactionManager platformTransactionManager;

    @Test
    void registrationHashesOutsideTheTransactionAndCommitsTheInsertInsideIt() {
        PasswordHashing observingPasswordHashing = mock(PasswordHashing.class);
        when(observingPasswordHashing.hash(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return passwordHashing.hash(invocation.getArgument(0));
        });

        AccountRepository observingAccountRepository = mock(AccountRepository.class);
        when(observingAccountRepository.insert(any(Account.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return accountRepository.insert(invocation.getArgument(0));
        });

        IdentityService identityService = new IdentityService(
                observingAccountRepository, observingPasswordHashing, clock, platformTransactionManager);
        Account account = identityService.register(new RegisterAccountCommand(email(), PASSWORD));

        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(accountRepository.findById(account.id())).isPresent();
    }

    @Test
    void failureAfterInsertRollsBackTheAccountAndAllowsRetry() {
        AccountRepository failingAccountRepository = mock(AccountRepository.class);
        when(failingAccountRepository.insert(any(Account.class))).thenAnswer(invocation -> {
            assertThat(accountRepository.insert(invocation.getArgument(0))).isTrue();
            throw new IllegalStateException("Synthetic failure after account insertion.");
        });

        String email = email();
        IdentityService failingIdentityService = new IdentityService(
                failingAccountRepository, passwordHashing, clock, platformTransactionManager);
        assertThatThrownBy(() -> failingIdentityService.register(new RegisterAccountCommand(email, PASSWORD)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM steelstory.identity_accounts WHERE email = ?", Integer.class, email)).isZero();

        IdentityService identityService = new IdentityService(
                accountRepository, passwordHashing, clock, platformTransactionManager);
        Account account = identityService.register(new RegisterAccountCommand(email, PASSWORD));

        assertThat(accountRepository.findById(account.id())).isPresent();
    }

    @Test
    void registrationLoginCsrfRotationAndLogoutUseDurableSessionsWithoutCredentialLeaks() throws Exception {
        Browser browser = new Browser(port);
        String email = email();
        String csrf = browser.csrf();
        String anonymousCookie = browser.sessionCookie();
        HttpResponse<String> registered = browser.post(
                "register", credentials("  " + email.toUpperCase() + "  ", PASSWORD), csrf);
        assertThat(registered.statusCode()).isEqualTo(201);
        assertThat(json(registered).path("email").asString()).isEqualTo(email);
        assertSafeAccount(registered);
        String id = json(registered).path("id").asString();
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM steelstory.identity_accounts WHERE email = ?",
                String.class, email)).startsWith("$argon2id$v=19$m=19456,t=2,p=1$").doesNotContain(PASSWORD);

        HttpResponse<String> login = browser.post("login", credentials(email, PASSWORD), csrf);
        assertThat(login.statusCode()).isEqualTo(200);
        assertSafeAccount(login);
        assertThat(json(login)).isEqualTo(json(registered));
        String authenticatedCookie = browser.sessionCookie();
        assertThat(authenticatedCookie).isNotEqualTo(anonymousCookie);
        assertThat(login.headers().allValues("set-cookie").toString())
                .containsIgnoringCase("HttpOnly").containsIgnoringCase("SameSite=Lax");
        assertThat(json(browser.get("/api/v1/auth/me?accountId=" + UUID.randomUUID())).path("id").asString())
                .isEqualTo(id);
        assertThat(browser.get("/api/v1/auth/me").headers().firstValue("cache-control").orElse(""))
                .contains("no-store");

        Browser stale = new Browser(port);
        assertThat(stale.getWithCookie("/api/v1/auth/me", anonymousCookie).statusCode()).isEqualTo(401);
        assertThat(browser.post("logout", "", csrf).statusCode()).isEqualTo(403);
        String freshCsrf = browser.csrf();
        assertThat(browser.post("logout", "", freshCsrf).statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(stale.getWithCookie("/api/v1/auth/me", authenticatedCookie).statusCode()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM steelstory.identity_sessions WHERE principal_name = ?",
                Integer.class, id)).isZero();
        assertThat(browser.post("login", credentials(email, PASSWORD), freshCsrf).statusCode()).isEqualTo(403);
        assertThat(browser.post("login", credentials(email, PASSWORD), browser.csrf()).statusCode()).isEqualTo(200);

        List<byte[]> attributes = jdbcTemplate.queryForList("""
                SELECT a.attribute_bytes FROM steelstory.identity_sessions_attributes a
                JOIN steelstory.identity_sessions s ON s.primary_id = a.session_primary_id
                WHERE s.principal_name = ?
                """, byte[].class, id);
        assertThat(attributes).isNotEmpty();
        for (byte[] bytes : attributes) {
            assertThat(new String(bytes, StandardCharsets.ISO_8859_1))
                    .doesNotContain(email, PASSWORD, "$argon2id$", "identity.internal.domain");
        }

        assertThat(jdbcTemplate.queryForObject("""
                SELECT max_inactive_interval FROM steelstory.identity_sessions WHERE principal_name = ?
                """, Integer.class, id)).isEqualTo(43200);
    }

    @Test
    void wrongPasswordUnknownAndDisabledAccountsHaveTheSameLoginFailure() throws Exception {
        Browser browser = new Browser(port);
        String email = email();
        String csrf = browser.csrf();
        assertThat(browser.post("register", credentials(email, PASSWORD), csrf).statusCode()).isEqualTo(201);
        HttpResponse<String> wrong = browser.post("login", credentials(email, "wrong synthetic password"), csrf);
        HttpResponse<String> missing = browser.post("login", credentials(email(), PASSWORD), csrf);
        jdbcTemplate.update(
                "UPDATE steelstory.identity_accounts SET status = 'DISABLED', updated_at = now() WHERE email = ?",
                email);
        HttpResponse<String> disabled = browser.post("login", credentials(email, PASSWORD), csrf);
        assertThat(List.of(wrong.statusCode(), missing.statusCode(), disabled.statusCode())).containsOnly(401);
        assertThat(json(wrong)).isEqualTo(json(missing)).isEqualTo(json(disabled));
        assertThat(wrong.body()).doesNotContain(email, PASSWORD, "DISABLED", "password_hash");
    }

    @Test
    void legacyPasswordCanLogInButCannotBeUsedForNewRegistration() throws Exception {
        String email = email();
        String legacyPassword = "old secret";
        PasswordHash passwordHash = passwordHashing.hash(legacyPassword);
        Account account = Account.register(new AccountId(UUID.randomUUID()), new EmailAddress(email),
                passwordHash, clock.instant());
        assertThat(accountRepository.insert(account)).isTrue();

        Browser browser = new Browser(port);
        String csrf = browser.csrf();
        assertThat(browser.post("register", credentials(email(), legacyPassword), csrf).statusCode()).isEqualTo(400);
        HttpResponse<String> login = browser.post("login", credentials(email, legacyPassword), csrf);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(json(login).path("id").asString()).isEqualTo(account.id().value().toString());
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM steelstory.identity_accounts WHERE email = ?", String.class, email))
                .isEqualTo(passwordHash.value());
    }

    @Test
    void loginInputErrorsAndCredentialFailuresDoNotDiscloseAccountExistence() throws Exception {
        Browser browser = new Browser(port);
        String csrf = browser.csrf();
        String activeEmail = email();
        String disabledEmail = email();
        String unknownEmail = email();
        assertThat(browser.post("register", credentials(activeEmail, PASSWORD), csrf).statusCode()).isEqualTo(201);
        assertThat(browser.post("register", credentials(disabledEmail, PASSWORD), csrf).statusCode()).isEqualTo(201);
        jdbcTemplate.update("UPDATE steelstory.identity_accounts SET status = 'DISABLED' WHERE email = ?",
                disabledEmail);

        for (String password : List.of("x", "wrong", " ", "😀", "a".repeat(128))) {
            HttpResponse<String> active = browser.post("login", credentials(activeEmail, password), csrf);
            HttpResponse<String> disabled = browser.post("login", credentials(disabledEmail, password), csrf);
            HttpResponse<String> unknown = browser.post("login", credentials(unknownEmail, password), csrf);
            assertThat(List.of(active.statusCode(), disabled.statusCode(), unknown.statusCode())).containsOnly(401);
            assertThat(json(active)).isEqualTo(json(disabled)).isEqualTo(json(unknown));
            assertThat(json(active).path("code").asString()).isEqualTo("INVALID_CREDENTIALS");
        }

        // Raw JSON escapes ensure malformed UTF-16 reaches the JSON decoder without client-side replacement.
        for (String passwordJson : List.of("null", "\"\"", "\"\\uD800\"", "\"\\uDC00\"",
                JSON.writeValueAsString("a".repeat(129)), JSON.writeValueAsString("😀".repeat(129)),
                JSON.writeValueAsString("a".repeat(257)))) {
            JsonNode expectedProblem = null;
            for (String email : List.of(activeEmail, disabledEmail, unknownEmail)) {
                String body = "{\"email\":\"" + email + "\",\"password\":" + passwordJson + "}";
                HttpResponse<String> response = browser.post("login", body, csrf);
                assertThat(response.statusCode()).isEqualTo(400);
                assertThat(json(response).path("code").asString()).isEqualTo("INVALID_INPUT");
                assertThat(response.body()).doesNotContain(email, PASSWORD, "DISABLED", "password_hash");
                if (expectedProblem == null) {
                    expectedProblem = json(response);
                } else {
                    assertThat(json(response)).isEqualTo(expectedProblem);
                }
            }
        }

        for (String email : List.of(activeEmail, disabledEmail, unknownEmail)) {
            assertThat(browser.post("login", JSON.writeValueAsString(Map.of("email", email)), csrf).statusCode())
                    .isEqualTo(400);
        }

        assertThat(browser.post("login", credentials("foo@@example.com", PASSWORD), csrf).statusCode()).isEqualTo(400);
        assertThat(browser.postChunked("login", credentials(activeEmail, "a".repeat(5000)), csrf).statusCode())
                .isEqualTo(413);
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void disabledAccountLosesExistingSessionAndModuleEligibility() throws Exception {
        Browser browser = new Browser(port);
        String email = email();
        String csrf = browser.csrf();
        HttpResponse<String> registered = browser.post("register", credentials(email, PASSWORD), csrf);
        AccountId id = new AccountId(UUID.fromString(json(registered).path("id").asString()));
        assertThat(accountAccess.isActive(id)).isTrue();
        assertThat(accountAccess.isActive(new AccountId(UUID.randomUUID()))).isFalse();
        assertThat(browser.post("login", credentials(email, PASSWORD), csrf).statusCode()).isEqualTo(200);
        jdbcTemplate.update(
                "UPDATE steelstory.identity_accounts SET status = 'DISABLED', updated_at = now() WHERE id = ?",
                id.value());
        assertThat(browser.get("/api/v1/status").statusCode()).isEqualTo(200);
        assertThat(accountAccess.isActive(id)).isFalse();
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM steelstory.identity_sessions WHERE principal_name = ?",
                Integer.class, id.value().toString())).isZero();
    }

    @Test
    void csrfProtectsEveryMutationAndUnknownEndpointsAreNotPublic() throws Exception {
        Browser browser = new Browser(port);
        String csrf = browser.csrf();
        for (String operation : List.of("register", "login", "logout")) {
            HttpResponse<String> missing = browser.post(operation, credentials(email(), PASSWORD), null);
            HttpResponse<String> invalid = browser.post(operation, credentials(email(), PASSWORD), "invalid");
            assertThat(missing.statusCode()).isEqualTo(403);
            assertThat(invalid.statusCode()).isEqualTo(403);
            assertThat(json(missing).path("code").asString()).isEqualTo("ACCESS_DENIED");
            assertThat(missing.headers().firstValue("content-type").orElse("")).contains("application/problem+json");
        }

        assertThat(browser.get("/api/v1/status").statusCode()).isEqualTo(200);
        assertThat(browser.get("/actuator/health").statusCode()).isEqualTo(200);
        assertThat(browser.get("/api/v1/unknown").statusCode()).isEqualTo(401);
        assertThat(browser.get("/actuator/env").statusCode()).isEqualTo(401);
        assertThat(browser.get("/v3/api-docs").statusCode()).isEqualTo(401);
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(browser.post("logout", "", csrf).statusCode()).isEqualTo(204);
    }

    @Test
    void validatesInputAndBoundsFixedLengthAndChunkedBodies() throws Exception {
        Browser browser = new Browser(port);
        String csrf = browser.csrf();
        for (String body : List.of("{}", "{", credentials("bad email", PASSWORD),
                credentials(email(), "a".repeat(14)), credentials(email(), "a".repeat(129)),
                credentials(email(), "😀".repeat(129)))) {
            HttpResponse<String> response = browser.post("register", body, csrf);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
            assertThat(response.body()).doesNotContain(PASSWORD);
        }

        assertThat(browser.post("register", credentials(email(), "a".repeat(15)), csrf).statusCode()).isEqualTo(201);
        assertThat(browser.post("register", credentials(email(), "😀".repeat(128)), csrf).statusCode()).isEqualTo(201);
        String oversized = credentials(email(), "a".repeat(5000));
        assertThat(browser.post("register", oversized, csrf).statusCode()).isEqualTo(413);
        assertThat(browser.postChunked("register", oversized, csrf).statusCode()).isEqualTo(413);
        assertThat(browser.postChunked("register", credentials(email(), PASSWORD), csrf).statusCode()).isEqualTo(201);
    }

    @Test
    void concurrentCanonicalRegistrationsProduceOneAccountAndOneConflict() throws Exception {
        String email = email();
        Browser first = new Browser(port);
        Browser second = new Browser(port);
        String firstCsrf = first.csrf();
        String secondCsrf = second.csrf();
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Integer> registerFirst = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return first.post("register", credentials(email, PASSWORD), firstCsrf).statusCode();
        };
        Callable<Integer> registerSecond = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return second.post("register", credentials(" " + email.toUpperCase() + " ", PASSWORD), secondCsrf)
                    .statusCode();
        };
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = executor.invokeAll(
                    List.of(registerFirst, registerSecond), 30, TimeUnit.SECONDS);
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201, 409);
        }

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM steelstory.identity_accounts WHERE email = ?",
                Integer.class, email)).isEqualTo(1);
        assertThat(first.post("register", credentials(email, PASSWORD), firstCsrf).statusCode()).isEqualTo(409);
    }

    @Test
    void expiredJdbcSessionCannotAuthenticate() throws Exception {
        Browser browser = new Browser(port);
        String email = email();
        String csrf = browser.csrf();
        HttpResponse<String> registered = browser.post("register", credentials(email, PASSWORD), csrf);
        assertThat(browser.post("login", credentials(email, PASSWORD), csrf).statusCode()).isEqualTo(200);
        jdbcTemplate.update("""
                UPDATE steelstory.identity_sessions SET last_access_time = 0, expiry_time = 1
                WHERE principal_name = ?
                """, json(registered).path("id").asString());
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void accountAndSessionSurviveApplicationRestartAndWorkAcrossInstances() throws Exception {
        Browser browser = new Browser(port);
        String email = email();
        String csrf = browser.csrf();
        HttpResponse<String> registered = browser.post("register", credentials(email, PASSWORD), csrf);
        assertThat(browser.post("login", credentials(email, PASSWORD), csrf).statusCode()).isEqualTo(200);
        String id = json(registered).path("id").asString();
        // Independent full application contexts share only PostgreSQL and the browser cookie.
        for (int restart = 0; restart < 2; restart++) {
            try (ConfigurableApplicationContext instance = new SpringApplicationBuilder(SteelstoryApplication.class)
                    .run(
                    "--server.port=0",
                    "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                    "--spring.datasource.username=" + POSTGRES.getUsername(),
                    "--spring.datasource.password=" + POSTGRES.getPassword(),
                    "--server.servlet.session.cookie.secure=false",
                    "--spring.main.banner-mode=off")) {
                int instancePort = ((WebServerApplicationContext) instance).getWebServer().getPort();
                browser.port = instancePort;
                assertThat(json(browser.get("/api/v1/auth/me")).path("id").asString()).isEqualTo(id);
                assertThat(browser.post("login", credentials(email, PASSWORD), browser.csrf()).statusCode())
                        .isEqualTo(200);
            }
        }

        browser.port = port;
        assertThat(json(browser.get("/api/v1/auth/me")).path("id").asString()).isEqualTo(id);
        assertThat(browser.post("logout", "", browser.csrf()).statusCode()).isEqualTo(204);
    }

    private static void assertSafeAccount(HttpResponse<String> response) {
        assertThat(response.body()).doesNotContain("password", "hash", "credentials", PASSWORD);
        assertThat(json(response).properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("id", "email", "status", "createdAt", "updatedAt");
    }

    private static String email() {
        return UUID.randomUUID() + "@example.com";
    }

    private static String credentials(String email, String password) {
        return JSON.writeValueAsString(Map.of("email", email, "password", password));
    }

    private static JsonNode json(HttpResponse<String> response) {
        return JSON.readTree(response.body());
    }

    private static final class Browser {
        private int port;
        private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient httpClient = HttpClient.newBuilder().cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10)).build();

        Browser(int port) {
            this.port = port;
        }

        String csrf() throws Exception {
            HttpResponse<String> response = get("/api/v1/auth/csrf");
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            assertThat(response.headers().firstValue("cache-control").orElse("")).contains("no-store");
            assertThat(json(response).path("headerName").asString()).isEqualTo("X-CSRF-TOKEN");
            return json(response).path("token").asString();
        }

        String sessionCookie() {
            return cookieManager.getCookieStore().getCookies().stream()
                    .filter(cookie -> cookie.getName().equals("SESSION") && !cookie.hasExpired())
                    .findFirst().orElseThrow().getValue();
        }

        HttpResponse<String> get(String path) throws Exception {
            return httpClient.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> getWithCookie(String path, String cookie) throws Exception {
            return httpClient.send(request(path).header("Cookie", "SESSION=" + cookie).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String operation, String body, String csrf) throws Exception {
            return post(operation, HttpRequest.BodyPublishers.ofString(body), csrf);
        }

        HttpResponse<String> postChunked(String operation, String body, String csrf) throws Exception {
            return post(operation, HttpRequest.BodyPublishers.ofInputStream(
                    () -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))), csrf);
        }

        private HttpResponse<String> post(String operation, HttpRequest.BodyPublisher body, String csrf)
                throws Exception {
            HttpRequest.Builder request = request("/api/v1/auth/" + operation)
                    .header("Content-Type", "application/json");
            if (csrf != null) {
                request.header("X-CSRF-TOKEN", csrf);
            }

            return httpClient.send(request.POST(body).build(), HttpResponse.BodyHandlers.ofString());
        }

        private HttpRequest.Builder request(String path) {
            return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(20));
        }
    }
}
