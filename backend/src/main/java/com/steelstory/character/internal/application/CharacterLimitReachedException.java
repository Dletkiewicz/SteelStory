package com.steelstory.character.internal.application;

public class CharacterLimitReachedException extends RuntimeException {
    public CharacterLimitReachedException() {
        super("An account may own at most 3 active characters.");
    }
}
