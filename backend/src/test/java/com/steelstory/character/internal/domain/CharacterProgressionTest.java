package com.steelstory.character.internal.domain;

import com.steelstory.identity.api.AccountId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CharacterProgressionTest {
    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

    @ParameterizedTest
    @CsvSource({"1,0,100", "2,100,400", "3,500,900", "4,1400,1600", "5,3000,2500",
            "10,28500,10000", "200,264670000,0"})
    void provisionalCurveUsesCumulativeThresholds(int level, long total, long incremental) {
        assertThat(ExperienceCurve.totalExperienceRequiredForLevel(level)).isEqualTo(total);
        assertThat(ExperienceCurve.experienceRequiredForNextLevel(level)).isEqualTo(incremental);
        assertThat(ExperienceCurve.levelForTotalExperience(total)).isEqualTo(level);
    }

    @Test
    void everyThresholdAwardsExactlyThreePointsAndNeverLevelsEarly() {
        PlayerCharacter original = beginner();
        assertThat(original.level()).isEqualTo(1);
        assertThat(original.experience()).isZero();
        assertThat(original.unspentTrainingPoints()).isZero();
        for (int level = 2; level <= 200; level++) {
            long threshold = ExperienceCurve.totalExperienceRequiredForLevel(level);
            PlayerCharacter before = original.gainExperience(threshold - 1, NOW);
            PlayerCharacter after = before.gainExperience(1, NOW.plusSeconds(1));
            assertThat(before.level()).isEqualTo(level - 1);
            assertThat(after.level()).isEqualTo(level);
            assertThat(after.unspentTrainingPoints()).isEqualTo(3 * (level - 1));
            assertThat(after.unspentTrainingPoints() - before.unspentTrainingPoints()).isEqualTo(3);
            assertThat(ExperienceCurve.progressWithinCurrentLevel(after.experience(), level)).isZero();
        }
    }

    @Test
    void multipleAndRepeatedIndependentRewardsPreserveIdentityAndAttributes() {
        PlayerCharacter original = beginner();
        PlayerCharacter partial = original.gainExperience(40, NOW);
        assertThat(partial.level()).isEqualTo(1);
        assertThat(partial.unspentTrainingPoints()).isZero();
        assertThat(ExperienceCurve.progressWithinCurrentLevel(partial.experience(), 1)).isEqualTo(40);
        PlayerCharacter levelThree = partial.gainExperience(460, NOW.plusSeconds(1));
        assertThat(levelThree.level()).isEqualTo(3);
        assertThat(levelThree.experience()).isEqualTo(500);
        assertThat(levelThree.unspentTrainingPoints()).isEqualTo(6);
        PlayerCharacter levelFour = levelThree.gainExperience(900, NOW.plusSeconds(2));
        assertThat(levelFour.level()).isEqualTo(4);
        assertThat(levelFour.unspentTrainingPoints()).isEqualTo(9);
        assertThat(levelFour.startingAttributes()).isEqualTo(original.startingAttributes());
        assertThat(levelFour.trainedAttributes()).isEqualTo(original.trainedAttributes());
        assertThat(levelFour.characterClass()).isEqualTo(original.characterClass());
        assertThat(levelFour.appearanceGender()).isEqualTo(original.appearanceGender());
        assertThat(levelFour.createdAt()).isEqualTo(original.createdAt());
        assertThat(original.experience()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void rejectsNonpositiveRewards(long amount) {
        assertThatThrownBy(() -> beginner().gainExperience(amount, NOW))
                .isInstanceOf(InvalidCharacterInputException.class);
    }

    @Test
    void hugeAwardsSaturateWithoutOverflowOrFurtherRewardsAtMaximum() {
        PlayerCharacter partial = beginner().gainExperience(40, NOW);
        PlayerCharacter maximum = partial.gainExperience(Long.MAX_VALUE, NOW.plusSeconds(1));
        assertThat(maximum.level()).isEqualTo(200);
        assertThat(maximum.experience()).isEqualTo(264_670_000L);
        assertThat(maximum.unspentTrainingPoints()).isEqualTo(597);
        assertThat(maximum.gainExperience(Long.MAX_VALUE, NOW.plusSeconds(2))).isSameAs(maximum);
    }

    @Test
    void deletedCharactersAndBackdatedUpdatesCannotGainExperience() {
        PlayerCharacter original = beginner();
        assertThatThrownBy(() -> original.delete(NOW).gainExperience(500, NOW))
                .isInstanceOf(InvalidCharacterInputException.class);
        assertThatThrownBy(() -> original.gainExperience(500, NOW.minusSeconds(1)))
                .isInstanceOf(InvalidCharacterInputException.class);
    }

    @Test
    void restorationValidatesConsistencyAndPreservesPreviouslyTrainedBonuses() {
        PlayerCharacter original = beginner();
        CharacterAttributes trained = new CharacterAttributes(3, 0, 0, 0, 0);
        PlayerCharacter restored = PlayerCharacter.restore(original.id(), original.accountId(), original.name(),
                original.characterClass(), original.appearanceGender(), original.startingAttributes(), trained,
                3, 500, 3, NOW, NOW);
        PlayerCharacter advanced = restored.gainExperience(900, NOW);
        assertThat(advanced.trainedAttributes()).isEqualTo(trained);
        assertThat(advanced.unspentTrainingPoints()).isEqualTo(6);
        for (long[] invalid : new long[][] {{1, 100, 0}, {3, 499, 6}, {201, 264670000, 597},
                {200, 264670001, 597}, {1, -1, 0}, {2, 100, -1}, {2, 100, 4}}) {
            assertThatThrownBy(() -> PlayerCharacter.restore(original.id(), original.accountId(), original.name(),
                    original.characterClass(), original.appearanceGender(), original.startingAttributes(), trained,
                    (int) invalid[0], invalid[1], (int) invalid[2], NOW, NOW))
                    .isInstanceOf(InvalidCharacterInputException.class);
        }
    }

    private PlayerCharacter beginner() {
        return PlayerCharacter.create(new CharacterId(UUID.randomUUID()), new AccountId(UUID.randomUUID()),
                new CharacterName("Novice"), CharacterClass.ARCHER, AppearanceGender.FEMALE,
                new StartingAllocation(new CharacterAttributes(0, 10, 0, 0, 0)), NOW);
    }
}
