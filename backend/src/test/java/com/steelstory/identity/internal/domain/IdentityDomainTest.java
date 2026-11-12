package com.steelstory.identity.internal.domain;

import com.steelstory.identity.api.AccountId;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityDomainTest {
    @Test
    void emailCanonicalizationIsLocaleIndependentAndPreservesProviderSpecificParts() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(new EmailAddress("  FIRST.Last+tag@EXAMPLE.COM\t"))
                    .isEqualTo(new EmailAddress("first.last+tag@example.com"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"x", "@example.com", ".a@example.com", "a.@example.com", "a..b@example.com",
            "a b@example.com", "a@-example.com", "a@example-.com", "a@example", "ż@example.com",
            "a@example.com\nX: value", "foo@@example.com", "foo@example..com", "foo@.com"})
    void rejectsInvalidEmails(String email) {
        assertThatThrownBy(() -> new EmailAddress(email)).isInstanceOf(InvalidIdentityInputException.class);
    }

    @Test
    void enforcesEmailLengthAndLabelBoundaries() {
        String domain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertThat(new EmailAddress("a".repeat(64) + "@" + domain).value()).hasSize(254);
        assertThatThrownBy(() -> new EmailAddress("a".repeat(65) + "@example.com"))
                .isInstanceOf(InvalidIdentityInputException.class);
        assertThatThrownBy(() -> new EmailAddress("a".repeat(64) + "@" + domain + "d"))
                .isInstanceOf(InvalidIdentityInputException.class);
        assertThatThrownBy(() -> new EmailAddress("a@" + "b".repeat(64) + ".com"))
                .isInstanceOf(InvalidIdentityInputException.class);
        assertThatThrownBy(() -> new EmailAddress(" ".repeat(321)))
                .isInstanceOf(InvalidIdentityInputException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "12345678901234", "\uD800abcdefghijklmno", "\uDC00abcdefghijklmno"})
    void rejectsInvalidPasswords(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validateNewPassword(password))
                .isInstanceOf(InvalidIdentityInputException.class);
    }

    @Test
    void passwordBoundariesCountUnicodeCodePointsAndDoNotTrim() {
        assertThatCode(() -> PasswordPolicy.validateNewPassword(" ".repeat(15))).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.validateNewPassword("a".repeat(128))).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.validateNewPassword("😀".repeat(128))).doesNotThrowAnyException();
        assertThatThrownBy(() -> PasswordPolicy.validateNewPassword("a".repeat(129)))
                .isInstanceOf(InvalidIdentityInputException.class);
        assertThatThrownBy(() -> PasswordPolicy.validateNewPassword("😀".repeat(129)))
                .isInstanceOf(InvalidIdentityInputException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "short", "12345678901234", " ", "😀"})
    void loginAcceptsLegacyPasswordsBelowTheNewPasswordMinimum(String password) {
        assertThatCode(() -> PasswordPolicy.validateLoginInput(password)).doesNotThrowAnyException();
        assertThatThrownBy(() -> PasswordPolicy.validateNewPassword(password))
                .isInstanceOf(InvalidIdentityInputException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"\uD800", "\uDC00", "a\uD800b", "a\uDC00b"})
    void loginRejectsMissingEmptyAndMalformedUnicodePasswords(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validateLoginInput(password))
                .isInstanceOf(InvalidIdentityInputException.class);
    }

    @Test
    void loginRetainsTheCodePointAndRawInputUpperBounds() {
        assertThatCode(() -> PasswordPolicy.validateLoginInput("a".repeat(128))).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.validateLoginInput("😀".repeat(128))).doesNotThrowAnyException();
        for (String password : new String[] {"a".repeat(129), "😀".repeat(129), "a".repeat(257)}) {
            assertThatThrownBy(() -> PasswordPolicy.validateLoginInput(password))
                    .isInstanceOf(InvalidIdentityInputException.class);
        }
    }

    @Test
    void accountProtectsIdentityAndTemporalInvariantsAndDisabledEligibility() {
        AccountId id = new AccountId(UUID.randomUUID());
        EmailAddress email = new EmailAddress("account@example.com");
        PasswordHash hash = new PasswordHash("$argon2id$v=19$m=19456,t=2,p=1$synthetic$synthetic");
        Instant now = Instant.parse("2026-11-12T00:00:00Z");
        assertThat(Account.register(id, email, hash, now).canAuthenticate()).isTrue();
        assertThat(Account.restore(id, email, hash, AccountStatus.DISABLED, now, now).canAuthenticate()).isFalse();
        assertThatThrownBy(() -> Account.restore(id, email, hash, AccountStatus.ACTIVE, now, now.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Account.register(null, email, hash, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AccountId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PasswordHash("plaintext")).isInstanceOf(IllegalArgumentException.class);
        assertThat(hash.toString()).doesNotContain(hash.value());
    }
}
