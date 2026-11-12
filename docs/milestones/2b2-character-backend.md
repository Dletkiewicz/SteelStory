# Milestone 2B.2: Character creation backend

Implemented 2026-11-12 under the explicit implementation request, superseding 2B.1's planning-only workflow. Scope stops at creation/list-own/get-own. See [ADR 0005](../adr/0005-character-creation.md).

## Files

New production files under backend/src/main/java/com/steelstory/character:

- package-info.java declares only identity::api.
- internal/domain: PlayerCharacter, CharacterId, CharacterName, CharacterClass, CharacterAttributes, StartingAllocation, InvalidCharacterInputException.
- internal/application: CharacterService, CreateCharacterCommand, CharacterRepository, CharacterLimitReachedException, CharacterNameTakenException, CharacterNotFoundException, InactiveAccountException.
- internal/infrastructure/JdbcCharacterRepository.java.
- internal/web: CharacterController, CharacterDtoMapper, CharacterExceptionHandler.
- internal/web/dto: CreateCharacterRequestDto, StartingAllocationRequestDto, CharacterAttributesResponseDto, CharacterResponseDto, CharacterListResponseDto, CharacterProblemResponseDto.

Other new files: backend/src/main/resources/db/migration/V0005__character_create_characters.sql; character tests CharacterDomainTest, CharacterIT and CharacterMigrationIT; ADR 0005 and this report.

Modified: backend/pom.xml (Checkstyle coverage), ArchitectureTest (module/dependency/domain/immutability verification), FoundationIT (V0005 and integer contract assertion), IdentityMigrationIT (explicitly target its original V0004 regression scope), docs/api/openapi.json, AGENTS.md, README.md, architecture/domain/development guidelines, ADR 0004 (no-Lombok convention), and both 2B.1 documents (authorization amendment). Existing Identity/DTO refactoring and user edits were preserved.

## Domain and persistence

Names preserve display capitalization, accept only 3–20 ASCII letters/digits with single internal spaces, and are globally unique ignoring ASCII case. The three classes are WARRIOR, ARCHER and MAGE with any combination per account. StartingAllocation requires exactly ten additional nonnegative points. Starting attributes are 5 plus allocation, totaling 35; trained bonuses are separately persisted at zero. Initial level is 1, experience and unspent training points are zero. The immutable aggregate validates restoration too and exposes no class-changing operation.

Creation locks a character-owned guard row per account, checks active eligibility through Identity's public API, counts and inserts in the same transaction. The cap is three, with no UNIQUE owner constraint. The database's generated canonical-name UNIQUE constraint resolves races across accounts. A conflict rolls back, preserving available slots. V0001–V0004 remain unchanged. Direct SQL is not an authorized application mutation path; all slot-changing operations must use the guard.

## API examples

Authenticate through the existing registration/login flow, retain SESSION, and fetch a fresh CSRF token after login. Send application/json and X-CSRF-TOKEN for POST /api/v1/characters:

```json
{
  "name": "Dark Knight",
  "characterClass": "WARRIOR",
  "startingAllocation": {
    "strength": 6,
    "dexterity": 0,
    "intelligence": 0,
    "constitution": 4,
    "luck": 0
  }
}
```

201 returns id, name, characterClass, level, experience, unspentTrainingPoints, startingAttributes, trainedAttributes, createdAt and updatedAt, plus Location. This example produces 11/5/5/9/5 starting attributes. Account ownership comes only from the principal; extra owner/progression request fields cannot change it.

GET /api/v1/characters returns {"characters":[...]} in createdAt/id order. GET /api/v1/characters/{id} returns the owned character or 404 for missing/unowned IDs. Invalid input is 400; duplicate name/limit is 409 with CHARACTER_NAME_TAKEN/CHARACTER_LIMIT_REACHED codes. Invalid input and missing records use INVALID_CHARACTER_INPUT/CHARACTER_NOT_FOUND. Authentication errors are 401; missing/invalid CSRF is 403. Framework errors may omit application codes.

## Verification

Commands used from backend: .\mvnw.cmd --batch-mode --no-transfer-progress test; verify; test-compile failsafe:integration-test "-Dit.test=FoundationIT" for contract export. Early runs found line-length violations and expected contract drift while adding endpoints. The exported allocation schema initially used strings; explicit Integer schema implementations and a contract regression assertion correct that. Existing OpenAPI paths, schemas and security schemes were structurally compared and retained before accepting the new snapshot.

Final .\mvnw.cmd --batch-mode --no-transfer-progress verify passed: 73 unit/architecture tests and 23 PostgreSQL integration tests, zero failures, errors or skips. This includes 26 character domain cases, six character HTTP/concurrency tests and one V0004-to-V0005 migration/constraint test. Checkstyle, Modulith, existing Identity/session tests, all five migrations and the updated OpenAPI comparison passed. The final log is backend/target/character-final-verify.log. The final diff check excluding the pre-existing ApiWebTest whitespace also passed.

Frontend checks (no source changes), with Node 24.22.2: npx --yes --package=npm@10.8.2 npm ci, npm run typecheck, npm run lint and npm run build all passed. npm ci reported zero vulnerabilities.

Toolchain/dependencies remain Temurin 25.0.5, Maven 3.9.16, Boot 4.1.2, Modulith 2.1.2, Security 7.1.2, Session JDBC 4.1.1, springdoc 3.2.0, Flyway 12.4.0, PostgreSQL JDBC 42.7.14, Testcontainers 2.0.5 and PostgreSQL 18.6 at the existing digest. No dependencies were added. Checkstyle Plugin 3.6.0 / engine 14.4.0 covers the new module.

## Limitations and local run

Account eligibility can race with a concurrent disable, as documented in ADR 0005 and the existing AccountAccess contract. No atomic status-lock guarantee is claimed. Deletion, training, progression, equipment, combat, respec, character frontend and production deployment remain deferred. No hosted CI or manual browser session was run.

Run docker compose up -d --wait at the repository root, then from backend run .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local". Use the existing README port overrides if 5432 is occupied. APIs run on port 8080; migrations apply on startup. Test the endpoints with the same cookie jar and CSRF flow documented in README.

Repository-wide git diff --check reports a pre-existing trailing space in the user's ApiWebTest edit, which was left untouched. Newly implemented files pass the scoped Checkstyle gate. No commit or push was performed.
