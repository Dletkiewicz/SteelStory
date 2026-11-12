# Steelstory — Milestone 2B.1: specyfikacja domeny postaci

> Implementation amendment: the subsequent explicit Milestone 2B.2 request authorizes the backend creation/list/get slice. Original planning-only statements below record the earlier phase. Frontend and later features remain deferred. See [implementation report](2b2-character-backend.md).

**Status:** zamknięte ustalenia projektowe dotyczące tworzenia postaci; mechaniki kolejnych etapów oznaczono osobno.  
**Data:** 12 listopada 2026  
**Kontekst:** po ukończeniu 2A Identity, na bazie Java 25, Spring Boot 4, Spring Modulith, Spring JDBC, PostgreSQL, Flyway, React/TypeScript.  
**Cel 2B:** bezpieczne utworzenie postaci, lista własnych postaci i ich podgląd. **Dokument nie oznacza zgody na implementację.**

## 1. Słownik

- **Konto (`Account`)** — podmiot uwierzytelniony w module `identity`.
- **Postać (`PlayerCharacter`)** — niezależny agregat należący do jednego konta.
- **Atrybuty** — `STR`, `DEX`, `INT`, `CON`, `LUK`.
- **Punkty startowe** — 10 punktów bezpłatnie rozdawanych w kreatorze ponad bazowe 5 w każdym atrybucie.
- **Punkty nauki** — punkty otrzymywane za awanse i wydawane u nauczycieli.
- **Trening** — trwałe zwiększenie atrybutu za punkt nauki oraz walutę gry.
- **Premie z wyposażenia** — nietrwałe modyfikatory atrybutów zależne od aktualnie założonych przedmiotów.

## 2. Ustalone reguły produktu

### 2.1. Konto i klasa

1. Jedno konto może mieć **maksymalnie 3 aktywne postacie**. Jest to stały limit, bez odblokowywania slotów.
2. Konto może mieć dowolne kombinacje klas, w tym trzy postacie tej samej klasy.
3. Klasy postaci: `WARRIOR`, `ARCHER`, `MAGE`.
4. Klasa jest wybierana przy tworzeniu i pozostaje niezmienna; respec atrybutów jej nie zmienia.
5. Każda postać ma unikalne UUID i należy do konkretnego `AccountId`.

### 2.2. Nazwy

1. Długość: **3–20 znaków**.
2. Dozwolone tylko ASCII: litery `A-Z`, `a-z`, cyfry `0-9` oraz **pojedyncza zwykła spacja między ciągami alfanumerycznymi**.
3. Zakazane: spacja na początku/końcu, wielokrotne spacje, tabulacje, znaki specjalne, emoji, polskie i inne nie-ASCII litery.
4. Nazwy są **unikalne globalnie, bez rozróżniania wielkości liter**: `Darek`, `darek` i `DAREK` są tą samą nazwą.
5. Zachowujemy oryginalną wielkość liter w wyświetlanej nazwie.
6. Przykładowa reguła walidacji: `^[A-Za-z0-9]+(?: [A-Za-z0-9]+)*$` oraz osobny warunek długości 3–20.
7. Nazwy zawierające same cyfry nie są obecnie zabronione; zmiana tej decyzji wymaga osobnego ustalenia.

### 2.3. Początek postaci

1. Każdy z pięciu atrybutów zaczyna z wartością **5**.
2. Gracz **rozdziela dokładnie 10 dodatkowych punktów** podczas tworzenia, za darmo.
3. Dowolny rozkład jest dozwolony, jeśli każdy przydział jest nieujemną liczbą całkowitą, a suma wynosi dokładnie 10.
4. Z samych zasad tworzenia wynika maksymalnie 15 w jednym atrybucie na poziomie 1 (5+10); **nie jest to ogólny limit atrybutu**.
5. Nowa postać ma `level = 1`, `experience = 0`, `unspentTrainingPoints = 0`.
6. Suma atrybutów trwałych na początku: **35** (5 × 5 + 10).

### 2.4. Rozwój po utworzeniu — zasady na przyszłość

1. Wstępnie maksymalny poziom to **200**; szczegółowe progi XP pozostają nieustalone.
2. Każdy awans o jeden poziom przyznaje **3 punkty nauki**, zatem od poziomu 1 do 200 dostępnych jest 199 × 3 = **597** punktów z awansów.
3. Poziom ogranicza łączną pulę możliwych do zdobycia punktów; **brak sztucznego limitu pojedynczego atrybutu**.
4. Punkty nauki można przechowywać i wydać później.
5. Punkty nauki po awansie wydaje się u nauczyciela odpowiedniego atrybutu: 1 punkt nauki → +1 trwałego atrybutu, z równoczesną zapłatą w walucie gry.
6. Cena +1 rośnie wraz z **bieżącą trwałą wartością trenowanego atrybutu**; premie z przedmiotów nie wpływają na cenę.
7. Przy treningu kilku punktów koszt liczy się krok po kroku, według kolejnych wartości atrybutu. Wzór, jednostka waluty, koszty i NPC zostaną określone później.
8. Przy poziomie 200 maksymalna suma atrybutów trwałych, po zainwestowaniu wszystkich punktów, to **632** (= 25 bazowych + 10 startowych + 597 z awansów). To **nie** jest limit ustawiany ręcznie ani maksimum pojedynczego atrybutu. Jeśli wszystkie bonusowe punkty trafiłyby w jeden atrybut, jego maksymalna wartość wynikająca z tej puli na poziomie 200 to 612 (pozostałe cztery nadal po 5), bez dodatkowego ograniczenia kodowego.

### 2.5. Reset atrybutów — zasady na przyszłość

1. Reset wymaga **specjalnego rzadkiego przedmiotu**; dostępność i nazwa przedmiotu pozostają do zaprojektowania.
2. Reset pozwala ponownie **bezpłatnie rozdzielić 10 punktów startowych**.
3. Punkty wcześniej wydane u nauczycieli wracają do puli dostępnych punktów nauki.
4. Wcześniej wydana waluta **nie jest zwracana**. Przy ponownym trenowaniu gracz płaci ponownie.
5. Respec nie zmienia klasy, doświadczenia ani poziomu.
6. Implementacja respecu nie należy do bieżącego Milestone 2B.

### 2.6. Usuwanie i przywracanie — zasady na przyszłość

1. Gracz może usunąć postać **dopiero po potwierdzeniu e-mailowym**.
2. Po skutecznym potwierdzeniu usunięcie jest **natychmiastowe z punktu widzenia gracza**, bez funkcji cofania.
3. Slot zostaje natychmiast zwolniony, ale dane są przechowywane przez określony czas, aby administrator mógł odzyskać postać np. po włamaniu.
4. Nazwa usuniętej postaci pozostaje **zarezerwowana do końca okresu retencji**.
5. Administracyjne odzyskanie wymaga odpowiednich uprawnień i audytu. Nie zakładamy przywracania całej bazy z backupu jako podstawowego mechanizmu.
6. Długość retencji, polityka prywatności, zachowanie przy pełnych trzech slotach i procedura odzyskiwania wymagają osobnych decyzji. **Nie przyjmować samodzielnie wartości 90 dni ani innego terminu**.
7. Moduł Identity 2A nie oferuje jeszcze kompletnej obsługi e-maili; usuwanie pozostaje poza 2B. Nie tworzyć teraz pozornych endpointów usuwania ani tabel tokenów.

### 2.7. Broń, atrybuty i wyposażenie — przyszłe moduły

| Klasa | Dozwolone kategorie broni | Główne skalowanie obrażeń |
|---|---|---|
| WARRIOR | Miecze, topory | STR |
| ARCHER | Łuki, kusze | DEX |
| MAGE | Różdżki, księgi czarów | INT |

- Dostęp do kategorii broni jest **ściśle przypisany do klasy**.
- Typy broni będą różnić się charakterystyką, jednak konkretne mechaniki nie są jeszcze ustalone.
- STR, DEX i INT mogą dawać także poboczne korzyści pozostałym klasom; szczegóły i formuły pozostają otwarte.
- CON służy przeżywalności, LUK efektom **bojowym**, w szczególności potencjalnym krytykom; LUK **nie wpływa na loot**.
- Przedmioty zapewniają premie **wyłącznie do pięciu atrybutów STR/DEX/INT/CON/LUK**, nie do HP, procentowych krytyków czy innych bezpośrednich bonusów bojowych. To nie wyklucza różnic w bazowych właściwościach broni, które będą zaprojektowane osobno.
- Koszt nauki bazuje na trwałych atrybutach postaci: **startowych + wytrenowanych**, nigdy na tymczasowych premiach.
- **Nie implementować** obliczania statystyk bojowych, lootów, dopuszczalności wyposażenia ani efektów przedmiotów w 2B.

## 3. Proponowany model domenowy 2B (decyzje techniczne do weryfikacji w repo)

**Agregat:** `PlayerCharacter` (nazwa odróżniona od `java.lang.Character`).

**Tożsamość i własność:** `CharacterId` (UUID), `AccountId` otrzymany z uwierzytelnionej sesji, `CharacterName`, `CharacterClass`.

**Stan rozwoju:** `level`, `experience`, `startingAttributes` (wartości bazowe po kreatorze), `trainedAttributes` (liczba dodatkowych punktów uzyskanych w treningu, początkowo 0), `unspentTrainingPoints` (początkowo 0); `createdAt`, `updatedAt` jako UTC `Instant`.

**Value Object:** `CharacterAttributes` jako pięć jawnych wartości całkowitych; oddzielna reprezentacja przydziału startowego (np. `StartingAllocation`) waliduje sumę 10, zamiast ufać klientowi, który mógłby przesłać gotowe atrybuty.

**Inwarianty:** każde początkowe pole ≥5, wytrenowane ≥0, punkty nauki ≥0, stan niemożliwy do zmiany przez publiczne settery. Serwis domenowy lub aplikacyjny kontroluje tworzenie, a agregat — spójność swojego stanu.

**Późniejszy odczyt bojowy:** `effectiveAttributes = startingAttributes + trainedAttributes + equipmentBonuses`. Nie utrwalać tej wartości jako kolejnego pola prawdy w `character`.

**Nie implementować na zapas:** drzewka dziedziczenia `Warrior extends PlayerCharacter`, rozbudowanego katalogu NPC, waluty, ekwipunku, pseudo-zdarzeń XP, komend debugowych awansu, statusów usunięcia i tokenów e-mail, jeśli nie służą teraz bieżącym przypadkom użycia. Stworzyć model rozszerzalny bez zgadywania przyszłych reguł.

## 4. Zakres funkcjonalny Milestone 2B — do wykonania w następnych etapach, NIE teraz

### W zakresie

- Tworzenie postaci (wybór nazwy, klasy, rozdział dokładnie 10 punktów).
- Odczyt listy **tylko własnych** postaci.
- Odczyt szczegółów **tylko własnej** postaci.
- Egzekwowanie limitu 3 aktywnych postaci na konto, także przy równoległych requestach.
- Globalna niepowtarzalność nazw bez uwzględniania wielkości liter, także przy wyścigach.
- Migracja Flyway, repozytorium Spring JDBC, OpenAPI, walidacja, testy domenowe/integracyjne/architektoniczne.
- Spójna obsługa błędów według konwencji Identity i ochrona sesji/CSRF.
- Minimalny ekran tworzenia/listy postaci w React **jako proponowany osobny podetap** — do uzgodnienia przed pracą nad frontendem.

### Poza zakresem

- Przyznawanie doświadczenia, awanse i szkolenia (poza przechowywaniem stanu początkowego).
- NPC, waluta, sklepy, ekonomia, ekwipunek, walka, buffy, looty i gildie.
- Reset statystyk, usuwanie postaci, e-mail, odzyskiwanie admina.
- Wybór postaci jako globalny stan serwerowej sesji.

## 5. Proponowany kontrakt HTTP (do zatwierdzenia po audycie repo)

| Metoda | Ścieżka | Operacja |
|---|---|---|
| `POST` | `/api/v1/characters` | Tworzenie własnej postaci, `201 Created` |
| `GET` | `/api/v1/characters` | Lista własnych postaci, `200 OK` |
| `GET` | `/api/v1/characters/{characterId}` | Szczegóły własnej postaci, `200 OK` |

**Przykładowe żądanie tworzenia:**

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

Backend sam dodaje po 5 do każdej wartości (wynik: STR 11, DEX 5, INT 5, CON 9, LUK 5). **Nie wolno klientowi przesyłać `ownerId`, `level`, `experience`, `trainedAttributes` ani `unspentTrainingPoints`.**

**Przykładowa odpowiedź `201`** powinna zawierać ID, nazwę, klasę, poziom, XP, atrybuty trwałe oraz daty. Dokładne nazwy pól DTO dopasować do istniejącego stylu API podczas przeglądu repo; nie dublować kontraktów.

**Błędy:** brak logowania `401`, niedozwolona/niepoprawna nazwa lub rozkład `400`, zajęta nazwa `409`, wyczerpany limit `409`, cudza lub nieistniejąca postać `404` (bez ujawniania jej istnienia), brak/niepoprawny CSRF na `POST` — zgodnie z zabezpieczeniami istniejącymi w repo. Konwencja `ProblemDetail`/kodów spójna z 2A.

**Lista:** deterministyczna kolejność, np. `createdAt ASC, id ASC`. Brak `GET` wszystkich postaci w systemie.

## 6. Architektura i bezpieczeństwo

1. Nowy moduł Spring Modulith `character`; zależność na **publiczne API `identity::api`** tylko tam, gdzie jest konieczna. Bez importów `identity.internal.*` ani zależności Identity → Character.
2. Uwierzytelniony `AccountId` pobierać ze zweryfikowanego principal/sesji; nie pobierać go z DTO ani parametru query.
3. Operacja tworzenia w **jednej transakcji**. Sugerowany wzorzec: tabela **należąca do modułu Character** z pojedynczym wierszem blokady/slotów na `AccountId` (`INSERT ... ON CONFLICT DO NOTHING`, następnie `SELECT ... FOR UPDATE`), sprawdzenie liczby aktywnych postaci, wstawienie. Nie wymaga dotykania wewnętrznych tabel Identity i pozwala bezpiecznie serializować próby tworzenia postaci na jednym koncie. Ostateczny wariant zweryfikować w kontekście istniejącej architektury.
4. Nazwy chronić także **unikalnym ograniczeniem w PostgreSQL** na kanonicznej reprezentacji `lower(name)`; sama walidacja w Javie nie zabezpiecza przed równoległymi żądaniami. Zapisać oryginalną pisownię. Przyszłe usunięte rekordy pozostają w rezerwacji do końca retencji.
5. SQL/inwarianty podstawowe: `NOT NULL`, ograniczenia nieujemności, enum/klasa jako jawnie walidowany tekst lub właściwy typ, ograniczenie 3 przez transakcję (nie `UNIQUE(account_id)`, bo relacja to 1:N).
6. Nowa migracja po `V0004` — nie edytować wcześniejszych wersji migracji, szczególnie zastosowanych lokalnie.
7. Nie wprowadzać JPA, Lomboka ani drugiego systemu uwierzytelniania. Używać istniejących stylów kodu, checkstyle, Spring JDBC, testów, OpenAPI.
8. Weryfikować brak IDOR: klient z konta B nie może poznać szczegółów postaci konta A.

## 7. Kryteria akceptacji 2B

- [ ] Rejestracja/logowanie z 2A nadal działają i sesja poprawnie rozpoznaje właściciela.
- [ ] Poprawne dane: utworzenie postaci z sumą przydziałów =10, stan `level=1`, `xp=0`, pięć atrybutów po doliczeniu 5.
- [ ] Błędy: niepoprawne ASCII, długość 2/21, nadmiarowe spacje, przydział 9/11, ujemny lub ułamkowy przydział, nieznana klasa.
- [ ] Kolizja `Darek`/`darek` w dwóch kontach kończy się 409, również przy wyścigu.
- [ ] Trzy dowolne postacie na jednym koncie dozwolone; czwarta kończy się 409.
- [ ] Dwie równoległe próby utworzenia trzeciej postaci nie przekraczają limitu (jedna sukces, jedna 409).
- [ ] Nieautoryzowany dostęp kończy się 401, cudza postać przy `GET /{id}` kończy się 404.
- [ ] POST chroniony istniejącym CSRF, GET działa po zalogowaniu.
- [ ] Baza utrwala stan bez możliwości fałszywego nadpisania ownera, levelu, XP czy liczby punktów przez klienta.
- [ ] Testy jednostkowe, integracyjne/Testcontainers, walidacja Modulith, Maven verify, Checkstyle i CI przechodzą.
- [ ] Dokumentacja OpenAPI i ADR/milestone aktualna; brak zmian w niezwiązanych modułach.

## 8. Kwestie odłożone świadomie

| Kwestia | Decyzja / dalszy krok |
|---|---|
| Level cap 200 | Wstępny — projektować tak, aby zmiana była prosta, bez arbitralnych limitów w bazie 2B |
| XP: progi i źródła | Ustalić przed implementacją awansów |
| NPC, monety, krzywa kosztu | Ustalić w etapie ekonomii i treningów |
| Dokładne role poboczne STR/DEX/INT/CON/LUK | Ustalić przy silniku walki; LUK nie dotyczy lootu |
| Bazowe różnice miecze/topory itd. | Ustalić w systemie broni |
| Retencja usuniętej postaci | Ustalić przed projektowaniem usuwania (bez domyślnej liczby dni) |
| Administracyjna odzyskiwalność przy pełnym limicie slotów | Zdefiniować procedurę przed wdrożeniem usuwania |
| UI kreatora | Proponowany podetap; ustalić poziom szczegółowości i design przed implementacją |

---

**Zasada prowadząca:** `Character` ma kontrolować swoją tożsamość i trwały rozwój; sprzęt, trenerzy, walka i ekonomia mają docelowo osobne odpowiedzialności. Milestone 2B dostarcza działający, testowalny fundament bez zgadywania przyszłego balansu.
