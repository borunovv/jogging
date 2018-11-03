package com.borunovv.jogging.users.util;

import com.borunovv.core.util.Assert;

public final class CredentialsValidator {

    private static final int LOGIN_MIN_LEN = 4;
    private static final int LOGIN_MAX_LEN = 30;
    private static final String LOGIN_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.@";

    private static final int PASSWORD_MIN_LEN = 8;
    private static final int PASSWORD_MAX_LEN = 30;
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.!@#$%^&*()+=";


    public static void validateCredentials(String login, String password) {
        ensureLenAndAlphabet("login", login, LOGIN_MIN_LEN, LOGIN_MAX_LEN, LOGIN_ALPHABET);
        ensureLenAndAlphabet("pass", password, PASSWORD_MIN_LEN, PASSWORD_MAX_LEN, PASSWORD_ALPHABET);
    }

    private static void ensureLenAndAlphabet(String paramName,
                                             String paramValue,
                                             int minLen,
                                             int maxLen,
                                             String alphabet) {
        Assert.isTrue(paramValue != null
                        && paramValue.length() >= minLen
                        && paramValue.length() < maxLen,
                String.format("Bad param '%s'. Expected string of %d-%d characters.", paramName, minLen, maxLen));

        for (int i = 0; i < paramValue.length(); ++i) {
            Assert.isTrue(alphabet.indexOf(paramValue.charAt(i)) >= 0,
                    String.format("Bad param '%s'. Wrong symbol: '%c'. Allowed symbols are: [%s].",
                            paramName, paramValue.charAt(i), alphabet));
        }
    }
}
