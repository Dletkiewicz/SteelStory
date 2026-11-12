package com.steelstory.identity.internal.domain;

public final class PasswordPolicy {
    private static final int NEW_PASSWORD_MIN_LENGTH = 15;
    private static final int MAX_LENGTH = 128;
    private static final String NEW_PASSWORD_LENGTH_MESSAGE = "Password must contain 15 to 128 Unicode characters.";
    private static final String LOGIN_INPUT_LENGTH_MESSAGE = "Password must contain 1 to 128 Unicode characters.";
    private static final String INVALID_UNICODE_MESSAGE = "Password contains invalid Unicode.";

    private PasswordPolicy() {
    }

    public static void validateNewPassword(String password) {
        validate(password, NEW_PASSWORD_MIN_LENGTH, NEW_PASSWORD_LENGTH_MESSAGE);
    }

    public static void validateLoginInput(String password) {
        validate(password, 1, LOGIN_INPUT_LENGTH_MESSAGE);
    }

    private static void validate(String password, int minimumLength, String invalidLengthMessage) {
        if (password == null || password.length() > MAX_LENGTH * 2) {
            throw new InvalidIdentityInputException(invalidLengthMessage);
        }

        int length = password.codePointCount(0, password.length());
        if (length < minimumLength || length > MAX_LENGTH) {
            throw new InvalidIdentityInputException(invalidLengthMessage);
        }

        // Reject malformed UTF-16 so different inputs cannot encode to the same replacement bytes.
        for (int index = 0; index < password.length(); index++) {
            char character = password.charAt(index);
            if (Character.isHighSurrogate(character)) {
                index++;
                if (index >= password.length() || !Character.isLowSurrogate(password.charAt(index))) {
                    throw new InvalidIdentityInputException(INVALID_UNICODE_MESSAGE);
                }
            } else if (Character.isLowSurrogate(character)) {
                throw new InvalidIdentityInputException(INVALID_UNICODE_MESSAGE);
            }
        }
    }
}
