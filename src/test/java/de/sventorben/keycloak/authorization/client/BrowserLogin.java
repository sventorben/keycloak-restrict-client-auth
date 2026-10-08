package de.sventorben.keycloak.authorization.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.sventorben.keycloak.authorization.client.TestConstants.REALM_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logs in via the browser flow by submitting the login form, to obtain an authorization code.
 * Cookies are passed on manually, since Keycloak marks them as secure, which Java's cookie handling does not send
 * over plain HTTP.
 */
class BrowserLogin {

    private static final Pattern LOGIN_FORM = Pattern.compile("<form[^>]*id=\"kc-form-login\"[^>]*>");
    private static final Pattern FORM_ACTION = Pattern.compile("action=\"([^\"]+)\"");
    private static final Pattern CODE = Pattern.compile("[?&]code=([^&#]+)");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private final String serverUrl;

    BrowserLogin(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    String authorizationCode(String client, String redirectUri, String username, String password)
        throws IOException, InterruptedException {
        HttpResponse<String> loginPage = httpClient.send(HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/realms/" + REALM_TEST + "/protocol/openid-connect/auth"
                + "?response_type=code&scope=openid&state=state"
                + "&client_id=" + encode(client)
                + "&redirect_uri=" + encode(redirectUri)))
            .GET()
            .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
            .uri(URI.create(loginFormAction(loginPage.body())))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", cookies(loginPage))
            .POST(HttpRequest.BodyPublishers.ofString(
                "username=" + encode(username) + "&password=" + encode(password) + "&credentialId="))
            .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(302);
        Matcher code = CODE.matcher(response.headers().firstValue("Location").orElseThrow());
        assertThat(code.find()).isTrue();
        return code.group(1);
    }

    private static String cookies(HttpResponse<String> response) {
        return response.headers().allValues("Set-Cookie").stream()
            .map(cookie -> cookie.split(";", 2)[0])
            .collect(Collectors.joining("; "));
    }

    private static String loginFormAction(String loginPage) {
        Matcher form = LOGIN_FORM.matcher(loginPage);
        assertThat(form.find()).as(loginPage).isTrue();
        Matcher action = FORM_ACTION.matcher(form.group());
        assertThat(action.find()).as(form.group()).isTrue();
        return action.group(1).replace("&amp;", "&");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
