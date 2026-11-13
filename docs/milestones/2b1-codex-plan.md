# Steelstory — plan dla Codexa po specyfikacji 2B.1

> Implementation amendment: the subsequent explicit Milestone 2B.2 request authorizes the backend creation/list/get slice. Original planning-only statements below record the earlier phase. Frontend and later features remain deferred. See [implementation report](2b2-character-backend.md).

**Stan:** wyłącznie planowanie. **Nie implementować, nie tworzyć gałęzi, nie wykonywać commitów ani push.**  
**Dokument źródłowy w repo:** `docs/milestones/2b1-character-spec.md`.

## A. Sposób pracy

1. Dzielimy zadanie na małe, weryfikowalne etapy, a nie jeden wielki prompt „napisz cały Characters”.
2. Codex najpierw czyta repo i przygotowuje **plan różnic względem istniejącej architektury**. Niczego nie zmienia bez zatwierdzenia.
3. Każdy etap implementacyjny w przyszłości kończy się raportem zmienionych plików, testami, ryzykami i pytaniami o niejednoznaczne mechaniki.
4. OOP/SOLID/DRY/KISS bez nadmiarowych abstrakcji; Java: jawne typy, czytelne nazwy, bez `var` i Lomboka, jawne importy, brak zbędnych klas i dziedziczenia.
5. Zachowujemy już ukończony `identity`, session + CSRF, kontrakt OpenAPI, Flyway i zakres jakości/Checkstyle.
6. Nie uruchamiać instalacji zależności w kółko, nie robić niezapowiedzianych aktualizacji technologii. Wykorzystywać istniejące polecenia z repo/CI.

## B. Fazy (uruchamiane w przyszłości osobnymi poleceniami)

### 0 — Audyt i plan (bez edycji)

- Sprawdzić `AGENTS.md`, README, ADR, `docs/milestones`, konfigurację Modulith, Identity API/security/principal, OpenAPI, migracje `V0001`–`V0004`, testy i build.
- Zidentyfikować zgodne style package naming, rekordów DTO, obsługi `ProblemDetail`, logowania i identyfikatora konta.
- Zaproponować dokładne pliki modułu `character`, migrację `V0005`, kontrakty HTTP i sposób blokowania limitu 3.
- Wypisać ryzyka i punkty wymagające decyzji — **bez kodowania**.

### 1 — Wewnętrzny model domenowy i testy jednostkowe

- `CharacterId`, `CharacterName`, `CharacterClass`, `CharacterAttributes`, `StartingAllocation`, `PlayerCharacter`.
- Walidacja nazwy ASCII 3–20, pojedyncze spacje, case-insensitive key; start 5+10, stan lvl1/XP0/punkty nauki0.
- Tylko zachowania naprawdę potrzebne do utworzenia postaci; bez publicznych setterów i fikcyjnych operacji treningu.
- Testy granic i niezmienników.
- Zaktualizować zakres Checkstyle, by nowy moduł objąć tym samym standardem Java co Identity.

### 2 — PostgreSQL/Flyway, repozytorium i limit slotów

- Nowa migracja `V0005` lub następny wolny numer, bez modyfikacji poprzednich.
- Tabele `characters` i potrzebny mechanizm synchronizacji per konto po weryfikacji architektury.
- Ograniczenia unikalności globalnych nazw bez rozróżniania wielkości liter, pełna integralność danych.
- Spring JDBC; atomowe sprawdzenie limitu 3 i zapis; właściwe mapowanie konfliktów unikalności.
- Testy wyścigów: dwa requesty walczące o ostatni slot; dwa requesty z tą samą nazwą.
- **Bez modelu usuwania, poczty, golda czy XP-awansów**.

### 3 — Serwis aplikacyjny i REST

- `POST /api/v1/characters`, `GET /api/v1/characters`, `GET /api/v1/characters/{characterId}`.
- `AccountId` wyłącznie z aktualnej autoryzowanej sesji/principala, bez `ownerId` w żądaniu.
- 201/200/400/401/404/409; zgodność z istniejącym formatem problemów i CSRF.
- Testy integracyjne z dwoma kontami, cookies, csrf, izolacją właściciela, konfliktem nazwy i limitem.
- OpenAPI, architektura Modulith i zgodność z Identity.

### 4 — Frontend kreatora i listy (podetap proponowany, do zatwierdzenia)

- Prosty ekran wyboru klasy, nazwy, rozdania 10 punktów i walidacji.
- Lista maksymalnie trzech postaci zalogowanego gracza oraz podgląd atrybutów.
- Użyć istniejącego stylu React/TS/Vite i kontraktu sesji/CSRF.
- Prawidłowe obsługiwanie 400/401/409, stanu ładowania, podwójnego submitu.
- Bez fikcyjnych przycisków „trenuj”, „usuń”, „wyposaż”, „walcz”.

### 5 — Ostateczna kontrola i przegląd

- Maven verify + Checkstyle + testy architektoniczne + integracyjne Testcontainers.
- npm ci/typecheck/lint/build, gdy frontend w zakresie; lokalny test Postman z sesją i CSRF.
- Przegląd równoległości transakcji, SQL, indeksów, modułów, odpowiedzi błędów i ścieżek autoryzacji.
- Porównanie implementacji z check-listą specyfikacji, dokumentacja zmian.
- Dopiero potem commit, push, PR, GitHub Actions i merge według dotychczasowego workflow.

## C. Proponowana struktura modułu (do skorygowania po audycie)

```text
backend/src/main/java/com/steelstory/character/
  api/                        # tylko jeśli inne moduły naprawdę potrzebują publicznych typów
  internal/
    domain/                    # PlayerCharacter, CharacterName, CharacterClass, ...
    application/               # CreateCharacterService, query services, ports
    infrastructure/            # JDBC repositories
    web/                       # controllers, DTO i exception handler
  package-info.java

backend/src/main/resources/db/migration/
  V0005__character_create_characters.sql

backend/src/test/java/com/steelstory/character/
  ...
```

Unikać kopiowania struktury na ślepo. Jeśli w istniejącym repo jest lepsza konwencja, najpierw ją opisać i uzasadnić.

## D. Gotowy prompt do Codexa — **teraz tylko plan, żadnych zmian**

Skopiuj treść od znacznika START do KONIEC po umieszczeniu specyfikacji w repo (np. w `docs/milestones/2b1-character-spec.md`).

--- START PROMPT ---

STEELSTORY — MILESTONE 2B.1 / REVIEW-ONLY PLANNING

Jesteś senior Java/Spring architektem pracującym w istniejącym repo Steelstory.
Milestone 2A Identity jest ukończony i scalony do main.

TWOJE ZADANIE TERAZ: TYLKO AUDYT I PLAN. NIE IMPLEMENTUJ NICZEGO.

1. Najpierw przeczytaj AGENTS.md, README, ADR, istniejące raporty milestone, kod identity i security, konfigurację Spring Modulith, migracje Flyway, CI oraz testy.
2. Przeczytaj dokument docs/milestones/2b1-character-spec.md. Jeżeli nie istnieje — zgłoś brak i nie zgaduj reguł RPG.
3. Sprawdź, jak z sesji uzyskać AccountId przez publiczne API identity, bez importowania identity.internal.*.
4. Zaproponuj minimalny model PlayerCharacter/Value Objects, schemat PostgreSQL, transakcje, bezpieczny limit 3, globalnie unikalne nazwy ASCII case-insensitive, REST DTO i testy.
5. Uwzględnij, że nie wolno implementować jeszcze XP, nauczycieli, waluty, ekwipunku, walki, respecu i usuwania postaci.
6. Zweryfikuj sposób podłączenia modułu character do Checkstyle, testów modułowości i OpenAPI.
7. Zaproponuj kolejność małych etapów z kryteriami DONE oraz dokładnymi poleceniami weryfikacyjnymi wynikającymi z tego repo.
8. Oznacz niezgodności z istniejącymi konwencjami, ryzyka transakcyjne i wszelkie nieustalone wymagania. Nie podejmuj decyzji biznesowych samodzielnie.

OGRANICZENIA:
- ZERO edycji plików, ZERO generowania kodu produkcyjnego/migracji, ZERO commit/push/branch.
- Nie uruchamiaj komend mutujących repo; nie instaluj zależności.
- Nie wyłączaj Spring Security/CSRF.
- Nie używaj Lomboka, JPA, `var` ani sztucznej hierarchii Warrior extends Character.
- OOP, SOLID, DRY, KISS, Java 25, Spring Boot 4, Spring JDBC, PostgreSQL, Flyway.

ODPOWIEDŹ:
A. Aktualne zastane konwencje i ograniczenia repo.
B. Proponowane klasy/porty/moduły/migracje/endpoints.
C. Strategia integralności i współbieżności z uzasadnieniem.
D. Plan implementacyjny 3–5 małych etapów.
E. Test plan, dokładne polecenia uruchomienia, potencjalne problemy.
F. Pytania wymagające decyzji przed rozpoczęciem kodowania.

--- KONIEC PROMPT ---

## E. Bramka startu implementacji

Codex kończy audyt i przedstawia plan → użytkownik zatwierdza zakres/ryzyka → dopiero wtedy tworzymy gałąź `feat/characters` z aktualnego `main` i osobnym promptem zlecamy etap 1.

**Uwaga:** „Milestone 2B.1” to obecnie faza projektowa. Powyższy plan nie autoryzuje jeszcze pisania kodu.
