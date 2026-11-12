package com.steelstory.character;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.session.cookie.secure=false")
class CharacterIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PATH = "/api/v1/characters";

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

    @Test
    void persistsInitialStateAndIsolatesOwners() throws Exception {
        Browser owner = signedIn();
        Browser other = signedIn();
        String name = name();
        HttpResponse<String> created = owner.post(PATH, body(name), owner.csrf());
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        JsonNode character = JSON.readTree(created.body());
        String path = PATH + "/" + character.path("id").asString();
        assertThat(created.headers().firstValue("location")).contains(path);
        assertThat(character.path("name").asString()).isEqualTo(name);
        assertThat(character.path("startingAttributes").path("strength").asInt()).isEqualTo(11);
        assertThat(character.path("startingAttributes").path("constitution").asInt()).isEqualTo(9);
        assertThat(character.path("trainedAttributes").path("strength").asInt()).isZero();
        assertThat(character.path("level").asInt()).isEqualTo(1);
        assertThat(character.path("experience").asLong()).isZero();
        assertThat(character.path("unspentTrainingPoints").asInt()).isZero();
        assertThat(JSON.readTree(owner.get(path).body())).isEqualTo(character);
        assertThat(JSON.readTree(owner.get(PATH).body()).path("characters").get(0)).isEqualTo(character);
        assertThat(JSON.readTree(other.get(PATH).body()).path("characters").size()).isZero();
        assertThat(other.get(path).statusCode()).isEqualTo(404);
        assertThat(owner.get(PATH + "/" + UUID.randomUUID()).statusCode()).isEqualTo(404);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT starting_strength FROM steelstory.character_characters WHERE id = ?",
                Integer.class, UUID.fromString(character.path("id").asString()))).isEqualTo(11);
    }

    @Test
    void duplicateNamesAndFourthCharacterConflictAndRetryDoesNotConsumeSlot() throws Exception {
        Browser first = signedIn();
        Browser second = signedIn();
        String name = name();
        assertThat(first.post(PATH, body(name), first.csrf()).statusCode()).isEqualTo(201);
        assertThat(second.post(PATH, body(name.toUpperCase(Locale.ROOT)), second.csrf()).statusCode())
                .isEqualTo(409);
        for (int index = 0; index < 3; index++) {
            assertThat(second.post(PATH, body(name()), second.csrf()).statusCode()).isEqualTo(201);
        }

        HttpResponse<String> fourth = second.post(PATH, body(name()), second.csrf());
        assertThat(fourth.statusCode()).isEqualTo(409);
        assertThat(fourth.body()).contains("CHARACTER_LIMIT_REACHED");
        assertThat(JSON.readTree(second.get(PATH).body()).path("characters").size()).isEqualTo(3);
    }

    @Test
    void rejectsInvalidInputAndProtectsServerOwnedState() throws Exception {
        Browser browser = signedIn();
        String csrf = browser.csrf();
        String valid = body(name());
        for (String invalid : List.of(
                valid.replace("WARRIOR", "ROGUE"), valid.replace("\"strength\":6", "\"strength\":-1"),
                valid.replace("\"strength\":6", "\"strength\":5"),
                valid.replace("\"strength\":6", "\"strength\":7"),
                valid.replace("\"strength\":6", "\"strength\":6.5"),
                valid.replace("\"strength\":6", "\"strength\":4294967302"),
                valid.replace("\"strength\":6", "\"strength\":null"),
                valid.replace("\"strength\":6,", ""), body("Bad  Name"), "{}")) {
            HttpResponse<String> response = browser.post(PATH, invalid, csrf);
            assertThat(response.statusCode()).as(invalid + " -> " + response.body()).isEqualTo(400);
        }

        String forged = valid.substring(0, valid.length() - 1)
                + ",\"accountId\":\"" + UUID.randomUUID() + "\",\"level\":99,\"experience\":999}";
        HttpResponse<String> created = browser.post(PATH, forged, csrf);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(JSON.readTree(created.body()).path("level").asInt()).isEqualTo(1);
        assertThat(JSON.readTree(browser.get(PATH).body()).path("characters").size()).isEqualTo(1);
    }

    @Test
    void authenticationCsrfAndDisabledAccountsRemainProtected() throws Exception {
        Browser anonymous = new Browser();
        assertThat(anonymous.get(PATH).statusCode()).isEqualTo(401);
        assertThat(anonymous.get(PATH + "/" + UUID.randomUUID()).statusCode()).isEqualTo(401);
        assertThat(anonymous.post(PATH, body(name()), anonymous.csrf()).statusCode()).isEqualTo(401);
        Browser browser = signedIn();
        assertThat(browser.post(PATH, body(name()), null).statusCode()).isEqualTo(403);
        assertThat(browser.post(PATH, body(name()), "invalid").statusCode()).isEqualTo(403);
        String csrf = browser.csrf();
        String id = JSON.readTree(browser.get("/api/v1/auth/me").body()).path("id").asString();
        jdbcTemplate.update("UPDATE steelstory.identity_accounts SET status = 'DISABLED' WHERE id = ?",
                UUID.fromString(id));
        assertThat(browser.post(PATH, body(name()), csrf).statusCode()).isEqualTo(401);
    }

    @Test
    void concurrentRequestsCannotTakeTheSameLastSlot() throws Exception {
        Browser browser = signedIn();
        assertThat(browser.post(PATH, body(name()), browser.csrf()).statusCode()).isEqualTo(201);
        assertThat(browser.post(PATH, body(name()), browser.csrf()).statusCode()).isEqualTo(201);
        List<Integer> statuses = race(browser, name(), browser, name());
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(JSON.readTree(browser.get(PATH).body()).path("characters").size()).isEqualTo(3);
    }

    @Test
    void concurrentFirstRequestsAndNamesAreSerialized() throws Exception {
        Browser first = signedIn();
        Browser second = signedIn();
        String name = name();
        assertThat(race(first, name, second, name.toUpperCase(Locale.ROOT)))
                .containsExactlyInAnyOrder(201, 409);
        Browser fresh = signedIn();
        assertThat(race(fresh, name(), fresh, name())).containsOnly(201);
        assertThat(JSON.readTree(fresh.get(PATH).body()).path("characters").size()).isEqualTo(2);
    }

    private List<Integer> race(Browser first, String firstName, Browser second, String secondName) throws Exception {
        String firstCsrf = first.csrf();
        String secondCsrf = second.csrf();
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<Integer> firstResult = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return first.post(PATH, body(firstName), firstCsrf).statusCode();
            });
            Future<Integer> secondResult = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return second.post(PATH, body(secondName), secondCsrf).statusCode();
            });
            return List.of(firstResult.get(30, TimeUnit.SECONDS), secondResult.get(30, TimeUnit.SECONDS));
        }
    }

    private Browser signedIn() throws Exception {
        Browser browser = new Browser();
        String credentials = JSON.writeValueAsString(
                Map.of("email", UUID.randomUUID() + "@example.test", "password", "synthetic test password"));
        String csrf = browser.csrf();
        assertThat(browser.post("/api/v1/auth/register", credentials, csrf).statusCode()).isEqualTo(201);
        assertThat(browser.post("/api/v1/auth/login", credentials, csrf).statusCode()).isEqualTo(200);
        return browser;
    }

    private static String name() {
        return "Hero " + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }

    private static String body(String name) {
        return "{\"name\":\"" + name + "\",\"characterClass\":\"WARRIOR\",\"startingAllocation\":"
                + "{\"strength\":6,\"dexterity\":0,\"intelligence\":0,\"constitution\":4,\"luck\":0}}";
    }

    private final class Browser {
        private final HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(10)).build();

        String csrf() throws Exception {
            HttpResponse<String> response = get("/api/v1/auth/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            return JSON.readTree(response.body()).path("token").asString();
        }

        HttpResponse<String> get(String path) throws Exception {
            return httpClient.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, String body, String csrf) throws Exception {
            HttpRequest.Builder request = request(path).header("Content-Type", "application/json");
            if (csrf != null) {
                request.header("X-CSRF-TOKEN", csrf);
            }

            return httpClient.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        private HttpRequest.Builder request(String path) {
            return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(20));
        }
    }
}
