package com.ojtsu26.elearning.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordThatSatisfiesEveryRule() {
        assertThat(PasswordPolicy.isStrong("Career@2026")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "NOLOWERCASE@1",
            "nouppercase@1",
            "NoNumber@",
            "NoSpecial123",
            "Short1!",
            "Contains Space1!"
    })
    void rejectsPasswordsThatBreakACompositionRule(String password) {
        assertThat(PasswordPolicy.isStrong(password)).isFalse();
    }

    @Test
    void rejectsNullAndPasswordsLongerThan72Characters() {
        assertThat(PasswordPolicy.isStrong(null)).isFalse();
        assertThat(PasswordPolicy.isStrong("Aa1!" + "x".repeat(69))).isFalse();
    }
}
