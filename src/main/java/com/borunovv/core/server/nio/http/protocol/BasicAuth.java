package com.borunovv.core.server.nio.http.protocol;

import com.borunovv.core.util.Assert;
import com.borunovv.core.util.CryptUtils;
import com.borunovv.core.util.StringUtils;

/**
 * Хелпер по проверке авторизации HTTP Basic auth.
 */
public final class BasicAuth {

    public static final String AUTH_HEADER_NAME = "Authorization";
    public static final String AUTH_HEADER_KEYWORD_BASIC = "Basic";

    private String login;
    private String password;


    public BasicAuth(String login, String password) {
        Assert.isTrue(!StringUtils.isNullOrEmpty(login), "Bad login: '" + login + "'");
        Assert.isTrue(!StringUtils.isNullOrEmpty(password), "Bad password: '" + password + "'");

        this.login = login;
        this.password = password;
    }

    /**
     * Проверяет авторизацию запроса. HTTP Basic auth.
     * Вернет true, если запрос авторизован.
     * Вернет false, если запрос не авторизован.
     * При этом в response выставится статус 401 и добавится Http-header 'WWW-Authenticate'
     */
    public boolean check(HttpRequest request, HttpResponse response) {
        String header = request.getSingleHeader(AUTH_HEADER_NAME);
        boolean result = false;
        if (header != null && header.contains(AUTH_HEADER_KEYWORD_BASIC)) {
            // Выделяем base-64 хвостик из строки вида 'Basic ABCDEF=='
            String encodedBase64LoginAndPassword = header.substring(
                    header.indexOf(AUTH_HEADER_KEYWORD_BASIC) + AUTH_HEADER_KEYWORD_BASIC.length())
                    .trim();

            // Декодируем логин и пароль (будет строка вида 'login:password')
            String decodedLoginAndPassword = CryptUtils.decodeBase64ToString(encodedBase64LoginAndPassword);

            String[] items = decodedLoginAndPassword.split(":");
            if (items.length == 2) {
                String loginFromRequest = items[0];
                String passwordFromRequest = items[1];
                result = login.equals(loginFromRequest)
                        && password.equals(passwordFromRequest);
            }
        }

        if (!result) {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Restricted\"");
        }

        return result;
    }

    // Вернет http-заголовок для Basic-авторизации в виде: 'Basic ABCD==',
    // где 'ABCD==' - зашифрованная (base64) строка 'login:password'.
    public static String makeAuthHeaderValue(String login, String password) {
        return AUTH_HEADER_KEYWORD_BASIC + " "
                + CryptUtils.encodeBase64(login + ":" + password);
    }
}
