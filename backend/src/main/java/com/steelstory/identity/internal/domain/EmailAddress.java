package com.steelstory.identity.internal.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record EmailAddress(String value) {
    private static final String INVALID_ADDRESS_MESSAGE = "A valid email address is required.";
    private static final Pattern ADDRESS = Pattern.compile(
            "[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
                    + "(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+");

    public EmailAddress {
        if (value == null || value.length() > 320) {
            throw new InvalidIdentityInputException(INVALID_ADDRESS_MESSAGE);
        }

        value = value.strip().toLowerCase(Locale.ROOT);
        int separator = value.indexOf('@');
        if (value.length() > 254 || separator < 1 || separator > 64
                || !ADDRESS.matcher(value).matches() || value.startsWith(".")
                || value.contains("..") || value.charAt(separator - 1) == '.') {
            throw new InvalidIdentityInputException(INVALID_ADDRESS_MESSAGE);
        }
    }
}
