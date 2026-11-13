package com.steelstory.character;

import com.steelstory.character.internal.application.CharacterNotFoundException;
import com.steelstory.character.internal.application.CharacterService;
import com.steelstory.character.internal.application.ExperienceAwardResult;
import com.steelstory.character.internal.domain.CharacterId;
import com.steelstory.character.internal.domain.InvalidCharacterInputException;
import com.steelstory.identity.api.AccountId;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.servlet.session.cookie.secure=false", "steelstory.dev.xp.enabled=true"})
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
    @Autowired CharacterService characterService;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void trustedRewardsPersistCumulativeProgressAndCapAtLevel200() throws Exception {
        Browser owner = signedIn();
        AccountId accountId = owner.accountId();
        JsonNode created = JSON.readTree(owner.post(PATH, body(name()), owner.csrf()).body());
        CharacterId characterId = new CharacterId(UUID.fromString(created.path("id").asString()));
        String path = PATH + "/" + characterId.value();
        long[] amounts = {40, 60, 400, 900};
        long[] totals = {40, 100, 500, 1400};
        int[] levels = {1, 2, 3, 4};
        for (int index = 0; index < amounts.length; index++) {
            ExperienceAwardResult result = characterService.awardExperience(accountId, characterId, amounts[index]);
            JsonNode saved = JSON.readTree(owner.get(path).body());
            assertThat(saved.path("experience").asLong()).isEqualTo(totals[index]);
            assertThat(saved.path("level").asInt()).isEqualTo(levels[index]);
            assertThat(saved.path("unspentTrainingPoints").asInt()).isEqualTo(3 * (levels[index] - 1));
            assertThat(saved.path("startingAttributes")).isEqualTo(created.path("startingAttributes"));
            assertThat(saved.path("trainedAttributes")).isEqualTo(created.path("trainedAttributes"));
            assertThat(result.acceptedExperience()).isEqualTo(amounts[index]);
        }

        ExperienceAwardResult maximum = characterService.awardExperience(accountId, characterId, Long.MAX_VALUE);
        assertThat(maximum.character().level()).isEqualTo(200);
        assertThat(maximum.character().unspentTrainingPoints()).isEqualTo(597);
        assertThat(maximum.acceptedExperience()).isEqualTo(264_670_000L - 1400);
        assertThat(characterService.awardExperience(accountId, characterId, Long.MAX_VALUE).levelsGained()).isZero();
        JsonNode saved = JSON.readTree(owner.get(path).body());
        assertThat(saved.path("experience").asLong()).isEqualTo(264_670_000L);
        assertThat(saved.path("progression").path("maximumLevel").asBoolean()).isTrue();
        assertThat(saved.path("progression").path("nextLevelExperience").asLong()).isZero();
        assertThat(saved.path("progression").path("remainingExperience").asLong()).isZero();
    }

    @Test
    void failedAwardsAndTransactionRollbackPreserveTheStoredCharacter() throws Exception {
        Browser owner = signedIn();
        AccountId accountId = owner.accountId();
        String id = JSON.readTree(owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString();
        CharacterId characterId = new CharacterId(UUID.fromString(id));
        String original = owner.get(PATH + "/" + id).body();
        assertThatThrownBy(() -> characterService.awardExperience(accountId, characterId, 0))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> characterService.awardExperience(accountId,
                new CharacterId(UUID.randomUUID()), 100))
                .isInstanceOf(CharacterNotFoundException.class);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            characterService.awardExperience(accountId, characterId, 500);
            throw new IllegalStateException("Force transaction rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(owner.get(PATH + "/" + id).body()).isEqualTo(original);
    }

    @Test
    void concurrentIndependentRewardsHaveNoLostXpOrDuplicateLevelPoints() throws Exception {
        Browser owner = signedIn();
        AccountId accountId = owner.accountId();
        CharacterId characterId = new CharacterId(UUID.fromString(JSON.readTree(
                owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString()));
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<ExperienceAwardResult> first = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return characterService.awardExperience(accountId, characterId, 100);
            });
            Future<ExperienceAwardResult> second = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return characterService.awardExperience(accountId, characterId, 400);
            });
            ExperienceAwardResult firstResult = first.get(30, TimeUnit.SECONDS);
            ExperienceAwardResult secondResult = second.get(30, TimeUnit.SECONDS);
            assertThat(firstResult.acceptedExperience() + secondResult.acceptedExperience()).isEqualTo(500);
            assertThat(firstResult.trainingPointsAwarded() + secondResult.trainingPointsAwarded()).isEqualTo(6);
        }

        JsonNode saved = JSON.readTree(owner.get(PATH + "/" + characterId.value()).body());
        assertThat(saved.path("experience").asLong()).isEqualTo(500);
        assertThat(saved.path("level").asInt()).isEqualTo(3);
        assertThat(saved.path("unspentTrainingPoints").asInt()).isEqualTo(6);
    }

    @Test
    void deletionAndAwardSerializeWithoutResurrectingCharacters() throws Exception {
        Browser owner = signedIn();
        AccountId accountId = owner.accountId();
        CharacterId characterId = new CharacterId(UUID.fromString(JSON.readTree(
                owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString()));
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<Boolean> award = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                try {
                    characterService.awardExperience(accountId, characterId, 500);
                    return true;
                } catch (CharacterNotFoundException exception) {
                    return false;
                }
            });
            Future<?> deletion = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                characterService.deleteOwn(accountId, characterId);
                return null;
            });
            boolean awarded = award.get(30, TimeUnit.SECONDS);
            deletion.get(30, TimeUnit.SECONDS);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT experience FROM steelstory.character_characters WHERE id = ? AND deleted_at IS NOT NULL",
                    Long.class, characterId.value())).isEqualTo(awarded ? 500L : 0L);
        }

        assertThat(owner.get(PATH + "/" + characterId.value()).statusCode()).isEqualTo(404);
        assertThatThrownBy(() -> characterService.awardExperience(accountId, characterId, 500))
                .isInstanceOf(CharacterNotFoundException.class);
    }

    @Test
    void developmentEndpointRequiresOwnerSessionCsrfAndValidBoundedXp() throws Exception {
        Browser owner = signedIn();
        Browser other = signedIn();
        Browser anonymous = new Browser();
        String id = JSON.readTree(owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString();
        String path = "/api/v1/dev/characters/" + id + "/grant-xp";
        String reward = "{\"amount\":500}";
        assertThat(anonymous.post(path, reward, anonymous.csrf()).statusCode()).isEqualTo(401);
        assertThat(owner.post(path, reward, null).statusCode()).isEqualTo(403);
        assertThat(owner.post(path, reward, "invalid").statusCode()).isEqualTo(403);
        assertThat(other.post(path, reward, other.csrf()).statusCode()).isEqualTo(404);
        for (String amount : List.of("0", "-1", "1.5", "1000000001", "1e100", "null", "{}", "[]")) {
            assertThat(owner.post(path, "{\"amount\":" + amount + "}", owner.csrf()).statusCode()).isEqualTo(400);
        }

        assertThat(owner.post(path, "{}", owner.csrf()).statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(owner.get(PATH + "/" + id).body()).path("experience").asLong()).isZero();
        HttpResponse<String> granted = owner.post(path, reward, owner.csrf());
        assertThat(granted.statusCode()).isEqualTo(200);
        JsonNode result = JSON.readTree(granted.body());
        assertThat(result.path("previousLevel").asInt()).isEqualTo(1);
        assertThat(result.path("currentLevel").asInt()).isEqualTo(3);
        assertThat(result.path("levelsGained").asInt()).isEqualTo(2);
        assertThat(result.path("trainingPointsAwarded").asInt()).isEqualTo(6);
        assertThat(JSON.readTree(owner.get(PATH + "/" + id).body()).path("experience").asLong()).isEqualTo(500);
        assertThat(owner.get("/v3/api-docs").body()).contains("grantDevelopmentExperience");
        assertThat(owner.delete(PATH + "/" + id, owner.csrf()).statusCode()).isEqualTo(204);
        assertThat(owner.post(path, reward, owner.csrf()).statusCode()).isEqualTo(404);
        assertThat(owner.post("/api/v1/dev/characters/" + UUID.randomUUID() + "/grant-xp", reward,
                owner.csrf()).statusCode()).isEqualTo(404);
        String csrf = owner.csrf();
        jdbcTemplate.update("UPDATE steelstory.identity_accounts SET status = 'DISABLED' WHERE id = ?",
                owner.accountId().value());
        assertThat(owner.post(path, reward, csrf).statusCode()).isEqualTo(401);
    }

    @Test
    void persistsBothAppearancesForEveryClassAndReturnsThemOnlyToTheOwner() throws Exception {
        Browser other = signedIn();
        for (String gender : List.of("MALE", "FEMALE")) {
            Browser owner = signedIn();
            for (String characterClass : List.of("WARRIOR", "ARCHER", "MAGE")) {
                String request = body(name()).replace("WARRIOR", characterClass)
                        .replace("\"startingAllocation\":", "\"appearanceGender\":\"" + gender
                                + "\",\"startingAllocation\":");
                HttpResponse<String> response = owner.post(PATH, request, owner.csrf());
                assertThat(response.statusCode()).isEqualTo(201);
                JsonNode created = JSON.readTree(response.body());
                assertThat(created.path("appearanceGender").asString()).isEqualTo(gender);
                assertThat(created.path("characterClass").asString()).isEqualTo(characterClass);
                assertThat(created.path("startingAttributes").path("strength").asInt()).isEqualTo(11);
                String id = created.path("id").asString();
                assertThat(JSON.readTree(owner.get(PATH + "/" + id).body())).isEqualTo(created);
                assertThat(other.get(PATH + "/" + id).statusCode()).isEqualTo(404);
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT appearance_gender FROM steelstory.character_characters WHERE id = ?",
                        String.class, UUID.fromString(id))).isEqualTo(gender);
            }

            JsonNode listed = JSON.readTree(owner.get(PATH).body()).path("characters");
            assertThat(listed.size()).isEqualTo(3);
            for (JsonNode character : listed) {
                assertThat(character.path("appearanceGender").asString()).isEqualTo(gender);
            }
        }
    }

    @Test
    void omittedOrNullAppearancePreservesLegacyClassDefaults() throws Exception {
        for (boolean explicitNull : List.of(false, true)) {
            Browser owner = signedIn();
            for (String characterClass : List.of("WARRIOR", "ARCHER", "MAGE")) {
                String request = body(name()).replace("WARRIOR", characterClass);
                if (explicitNull) {
                    request = request.replace("\"startingAllocation\":",
                            "\"appearanceGender\":null,\"startingAllocation\":");
                }

                HttpResponse<String> response = owner.post(PATH, request, owner.csrf());
                assertThat(response.statusCode()).isEqualTo(201);
                assertThat(JSON.readTree(response.body()).path("appearanceGender").asString())
                        .isEqualTo(characterClass.equals("ARCHER") ? "FEMALE" : "MALE");
            }
        }
    }

    @Test
    void invalidAppearanceIsRejectedWithoutConsumingASlot() throws Exception {
        Browser owner = signedIn();
        for (String value : List.of("\"female\"", "\"OTHER\"", "\" MALE\"", "\"\"", "42", "{}", "[]")) {
            String request = body(name()).replace("\"startingAllocation\":",
                    "\"appearanceGender\":" + value + ",\"startingAllocation\":");
            assertThat(owner.post(PATH, request, owner.csrf()).statusCode()).isEqualTo(400);
        }

        assertThat(JSON.readTree(owner.get(PATH).body()).path("characters").size()).isZero();
    }

    @Test
    void softDeletionHidesCharacterFreesSlotAndKeepsItsNameReserved() throws Exception {
        Browser owner = signedIn();
        Browser other = signedIn();
        String reservedName = name();
        String id = JSON.readTree(owner.post(PATH, body(reservedName), owner.csrf()).body()).path("id").asString();
        String path = PATH + "/" + id;
        for (int index = 0; index < 2; index++) {
            assertThat(owner.post(PATH, body(name()), owner.csrf()).statusCode()).isEqualTo(201);
        }

        assertThat(other.delete(path, other.csrf()).statusCode()).isEqualTo(404);
        assertThat(owner.get(path).statusCode()).isEqualTo(200);
        HttpResponse<String> deleted = owner.delete(path, owner.csrf());
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(deleted.body()).isEmpty();
        assertThat(owner.get(path).statusCode()).isEqualTo(404);
        assertThat(owner.delete(path, owner.csrf()).statusCode()).isEqualTo(404);
        assertThat(owner.delete(PATH + "/" + UUID.randomUUID(), owner.csrf()).statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(owner.get(PATH).body()).path("characters").size()).isEqualTo(2);
        assertThat(owner.get(PATH).body()).doesNotContain(id);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM steelstory.character_characters
                WHERE id = ? AND deleted_at IS NOT NULL AND deleted_at = updated_at
                """, Integer.class, UUID.fromString(id))).isEqualTo(1);
        HttpResponse<String> nameConflict = other.post(PATH,
                body(reservedName.toUpperCase(Locale.ROOT)), other.csrf());
        assertThat(nameConflict.statusCode()).isEqualTo(409);
        assertThat(nameConflict.body()).contains("CHARACTER_NAME_TAKEN");
        assertThat(owner.post(PATH, body(name()), owner.csrf()).statusCode()).isEqualTo(201);
        assertThat(owner.post(PATH, body(name()), owner.csrf()).statusCode()).isEqualTo(409);
    }

    @Test
    void deletionRequiresAuthenticationAndCsrf() throws Exception {
        Browser owner = signedIn();
        String id = JSON.readTree(owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString();
        String path = PATH + "/" + id;
        Browser anonymous = new Browser();
        assertThat(anonymous.delete(path, anonymous.csrf()).statusCode()).isEqualTo(401);
        assertThat(owner.delete(path, null).statusCode()).isEqualTo(403);
        assertThat(owner.delete(path, "invalid").statusCode()).isEqualTo(403);
        assertThat(owner.delete(PATH + "/invalid-id", owner.csrf()).statusCode()).isEqualTo(400);
        assertThat(owner.get(path).statusCode()).isEqualTo(200);
        String csrf = owner.csrf();
        UUID accountId = UUID.fromString(JSON.readTree(owner.get("/api/v1/auth/me").body()).path("id").asString());
        jdbcTemplate.update("UPDATE steelstory.identity_accounts SET status = 'DISABLED' WHERE id = ?", accountId);
        assertThat(owner.delete(path, csrf).statusCode()).isEqualTo(401);
    }

    @Test
    void concurrentDeletionHasOneEffect() throws Exception {
        Browser owner = signedIn();
        String id = JSON.readTree(owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString();
        String csrf = owner.csrf();
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return owner.delete(PATH + "/" + id, csrf).statusCode();
            });
            Future<Integer> second = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return owner.delete(PATH + "/" + id, csrf).statusCode();
            });
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204, 404);
        }

        assertThat(JSON.readTree(owner.get(PATH).body()).path("characters").size()).isZero();
    }

    @Test
    void deletionAndCreationPreserveTheActiveCap() throws Exception {
        Browser owner = signedIn();
        String id = JSON.readTree(owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString();
        for (int index = 0; index < 2; index++) {
            assertThat(owner.post(PATH, body(name()), owner.csrf()).statusCode()).isEqualTo(201);
        }

        String csrf = owner.csrf();
        CyclicBarrier barrier = new CyclicBarrier(3);
        try (ExecutorService executorService = Executors.newFixedThreadPool(3)) {
            Future<Integer> deletion = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return owner.delete(PATH + "/" + id, csrf).statusCode();
            });
            Future<Integer> first = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return owner.post(PATH, body(name()), csrf).statusCode();
            });
            Future<Integer> second = executorService.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return owner.post(PATH, body(name()), csrf).statusCode();
            });
            assertThat(deletion.get(30, TimeUnit.SECONDS)).isEqualTo(204);
            List<Integer> creations = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertThat(creations).allMatch(status -> status == 201 || status == 409);
            assertThat(creations.stream().filter(status -> status == 201).count()).isLessThanOrEqualTo(1);
        }

        int count = JSON.readTree(owner.get(PATH).body()).path("characters").size();
        assertThat(count).isBetween(2, 3);
        if (count == 2) {
            assertThat(owner.post(PATH, body(name()), csrf).statusCode()).isEqualTo(201);
        }

        assertThat(owner.post(PATH, body(name()), csrf).statusCode()).isEqualTo(409);
    }

    @Test
    void rolledBackDeletionKeepsTheCharacterPlayable() throws Exception {
        Browser owner = signedIn();
        String id = JSON.readTree(owner.post(PATH, body(name()), owner.csrf()).body()).path("id").asString();
        AccountId accountId = new AccountId(UUID.fromString(
                JSON.readTree(owner.get("/api/v1/auth/me").body()).path("id").asString()));
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            characterService.deleteOwn(accountId, new CharacterId(UUID.fromString(id)));
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT deleted_at IS NOT NULL FROM steelstory.character_characters WHERE id = ?
                    """, Boolean.class, UUID.fromString(id))).isTrue();
            status.setRollbackOnly();
        });

        assertThat(owner.get(PATH + "/" + id).statusCode()).isEqualTo(200);
        assertThat(owner.delete(PATH + "/" + id, owner.csrf()).statusCode()).isEqualTo(204);
    }

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

        AccountId accountId() throws Exception {
            return new AccountId(UUID.fromString(JSON.readTree(get("/api/v1/auth/me").body()).path("id").asString()));
        }

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

        HttpResponse<String> delete(String path, String csrf) throws Exception {
            HttpRequest.Builder request = request(path);
            if (csrf != null) {
                request.header("X-CSRF-TOKEN", csrf);
            }

            return httpClient.send(request.DELETE().build(), HttpResponse.BodyHandlers.ofString());
        }

        private HttpRequest.Builder request(String path) {
            return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(20));
        }
    }
}
