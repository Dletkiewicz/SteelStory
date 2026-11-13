package com.steelstory.character.internal.domain;

import com.steelstory.identity.api.AccountId;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CharacterDomainTest {
    @Test
    void deletionIsImmutablePreservesHistoryAndRejectsRepeatedOrBackdatedDeletion() {
        Instant createdAt = Instant.parse("2026-11-13T10:00:00Z");
        PlayerCharacter original = PlayerCharacter.create(new CharacterId(UUID.randomUUID()),
                new AccountId(UUID.randomUUID()), new CharacterName("Retired Hero"), CharacterClass.WARRIOR,
                AppearanceGender.FEMALE,
                new StartingAllocation(new CharacterAttributes(6, 0, 0, 4, 0)), createdAt);
        Instant deletedAt = createdAt.plusSeconds(60);
        PlayerCharacter deleted = original.delete(deletedAt);

        assertThat(original.deletedAt()).isNull();
        assertThat(deleted.deletedAt()).isEqualTo(deletedAt);
        assertThat(deleted.updatedAt()).isEqualTo(deletedAt);
        assertThat(deleted.createdAt()).isEqualTo(createdAt);
        assertThat(deleted.id()).isEqualTo(original.id());
        assertThat(deleted.name()).isEqualTo(original.name());
        assertThat(deleted.appearanceGender()).isEqualTo(AppearanceGender.FEMALE);
        assertThat(deleted.startingAttributes()).isEqualTo(original.startingAttributes());
        assertThatThrownBy(() -> deleted.delete(deletedAt.plusSeconds(1)))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> original.delete(createdAt.minusSeconds(1)))
                .isInstanceOf(InvalidCharacterInputException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"WARRIOR", "ARCHER", "MAGE"})
    void createsInitialStateWithSeparateTraining(String className) {
        CharacterClass characterClass = CharacterClass.parse(className);
        PlayerCharacter character = PlayerCharacter.create(new CharacterId(UUID.randomUUID()),
                new AccountId(UUID.randomUUID()), new CharacterName("Dark Knight"), characterClass,
                AppearanceGender.MALE,
                new StartingAllocation(new CharacterAttributes(6, 0, 0, 4, 0)), Instant.now());

        assertThat(character.characterClass()).isEqualTo(characterClass);
        assertThat(character.startingAttributes()).isEqualTo(new CharacterAttributes(11, 5, 5, 9, 5));
        assertThat(character.trainedAttributes()).isEqualTo(new CharacterAttributes(0, 0, 0, 0, 0));
        assertThat(character.level()).isEqualTo(1);
        assertThat(character.experience()).isZero();
        assertThat(character.unspentTrainingPoints()).isZero();
        assertThat(character.updatedAt()).isEqualTo(character.createdAt());
    }

    @Test
    void validatesEveryAllocationAndUsesOverflowSafeTotal() {
        assertThat(new StartingAllocation(new CharacterAttributes(0, 0, 10, 0, 0)).startingAttributes())
                .isEqualTo(new CharacterAttributes(5, 5, 15, 5, 5));
        for (int total : new int[] {0, 9, 11, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> new StartingAllocation(new CharacterAttributes(total, 0, 0, 0, 0)))
                    .isInstanceOf(InvalidCharacterInputException.class);
        }

        assertThatThrownBy(() -> new StartingAllocation(
                new CharacterAttributes(Integer.MAX_VALUE, Integer.MAX_VALUE, 12, 0, 0)))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> new CharacterAttributes(-1, 0, 0, 0, 0))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> new CharacterAttributes(0, -1, 0, 0, 0))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> new CharacterAttributes(0, 0, -1, 0, 0))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> new CharacterAttributes(0, 0, 0, -1, 0))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> new CharacterAttributes(0, 0, 0, 0, -1))
                .isInstanceOf(InvalidCharacterInputException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "Dark Knight", "AbC", "ABCDEFGHIJKLMNOPQRST"})
    void preservesNamesAndCanonicalizesAscii(String name) {
        CharacterName characterName = new CharacterName(name);
        assertThat(characterName.value()).isEqualTo(name);
        assertThat(new CharacterName(name.toUpperCase(Locale.ROOT)).canonical())
                .isEqualTo(characterName.canonical());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "ABCDEFGHIJKLMNOPQRSTU", " Dark", "Dark ", "Dark  Knight",
            "Dark_Knight", "Dark\tKnight", "Dark\nKnight", "Dąrek", "😀abc"})
    void rejectsInvalidNames(String name) {
        assertThatThrownBy(() -> new CharacterName(name)).isInstanceOf(InvalidCharacterInputException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"warrior", "ROGUE", " MAGE"})
    void rejectsInvalidClasses(String value) {
        assertThatThrownBy(() -> CharacterClass.parse(value)).isInstanceOf(InvalidCharacterInputException.class);
    }

    @Test
    void reconstitutionCannotBypassStartingInvariants() {
        assertThatThrownBy(() -> PlayerCharacter.restore(new CharacterId(UUID.randomUUID()),
                new AccountId(UUID.randomUUID()), new CharacterName("Valid Name"), CharacterClass.MAGE,
                AppearanceGender.FEMALE,
                new CharacterAttributes(4, 6, 5, 5, 15), new CharacterAttributes(0, 0, 0, 0, 0),
                1, 0, 0, Instant.now(), Instant.now())).isInstanceOf(InvalidCharacterInputException.class);
    }
}
