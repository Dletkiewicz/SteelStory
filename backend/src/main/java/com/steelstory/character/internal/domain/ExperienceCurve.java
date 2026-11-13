package com.steelstory.character.internal.domain;

/** Provisional development balance. Rebalancing requires an explicit existing-character policy. */
public final class ExperienceCurve {
    public static final int MAXIMUM_LEVEL = 200;
    public static final long MAXIMUM_EXPERIENCE = totalExperienceRequiredForLevel(MAXIMUM_LEVEL);

    private ExperienceCurve() {
    }

    public static long experienceRequiredForNextLevel(int level) {
        requireLevel(level);
        return level == MAXIMUM_LEVEL ? 0 : 100L * level * level;
    }

    public static long totalExperienceRequiredForLevel(int level) {
        requireLevel(level);
        long previous = level - 1L;
        // Inputs are bounded to 199; every intermediate fits comfortably in a long.
        return 100L * previous * (previous + 1) * (2 * previous + 1) / 6;
    }

    public static int levelForTotalExperience(long experience) {
        if (experience < 0 || experience > MAXIMUM_EXPERIENCE) {
            throw new InvalidCharacterInputException("Total experience is outside the progression bounds.");
        }

        int level = 1;
        while (level < MAXIMUM_LEVEL && experience >= totalExperienceRequiredForLevel(level + 1)) {
            level++;
        }

        return level;
    }

    public static long progressWithinCurrentLevel(long experience, int level) {
        if (levelForTotalExperience(experience) != level) {
            throw new InvalidCharacterInputException("Level must match total experience.");
        }

        return experience - totalExperienceRequiredForLevel(level);
    }

    private static void requireLevel(int level) {
        if (level < 1 || level > MAXIMUM_LEVEL) {
            throw new InvalidCharacterInputException("Level must be between 1 and 200.");
        }
    }
}
