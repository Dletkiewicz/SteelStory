package com.steelstory;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "springdoc.api-docs.enabled=true")
class FoundationIT {
    @Container
    // The digest was inspected from the official postgres:18.6 image.
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
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test
    void contextStartsAgainstPostgresAndMigrationsAreRepeatable() {
        assertThat(jdbc.queryForObject("select count(*) from information_schema.schemata where schema_name = 'steelstory'", Integer.class)).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("0005");
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class)).isEqualTo(5);
    }

    @Test
    void defaultSessionCookieRequiresHttps() {
        ResponseEntity<String> response = RestClient.create("http://localhost:" + port).get().uri("/api/v1/auth/csrf")
                .retrieve().toEntity(String.class);
        assertThat(response.getHeaders().get("Set-Cookie").toString())
                .contains("Secure", "HttpOnly", "SameSite=Lax", "Path=/");
    }

    @Test
    void servesStatusHealthAndOpenApiContract() throws Exception {
        RestClient client = RestClient.create("http://localhost:" + port);
        assertThat(client.get().uri("/api/v1/status").retrieve().body(String.class))
                .contains("steelstory", "UP");
        assertThat(client.get().uri("/actuator/health").retrieve().body(String.class)).contains("UP");
        String contract = client.get().uri("/v3/api-docs").retrieve().body(String.class);
        assertThat(contract).contains("/api/v1/status", "getStatus", "StatusResponse").doesNotContain("/actuator/");
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/openapi.json"), contract);
        JsonMapper mapper = JsonMapper.builder().build();
        for (String attribute : new String[] {"strength", "dexterity", "intelligence", "constitution", "luck"}) {
            assertThat(mapper.readTree(contract).path("components").path("schemas")
                    .path("StartingAllocationRequestDto").path("properties").path(attribute).path("type").asString())
                    .isEqualTo("integer");
        }

        assertThat(mapper.readTree(contract)).isEqualTo(
                mapper.readTree(Files.readString(Path.of("../docs/api/openapi.json"))));
    }
}
