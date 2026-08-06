package com.ojtsu26.elearning.validation;

import java.util.regex.Pattern;

/** Shared server-side password policy for account creation and password changes. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,72}$"
    );

    private PasswordPolicy() {
    }

    public static boolean isStrong(String value) {
        return value != null && STRONG_PASSWORD.matcher(value).matches();
    }
}
