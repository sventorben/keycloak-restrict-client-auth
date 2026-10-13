package de.sventorben.keycloak.authorization.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.sventorben.keycloak.authorization.client.TestConstants.REALM_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logs in via the browser flow by submitting forms, e.g. to obtain an authorization code.
 * Redirects within Keycloak, e.g. to required actions, are followed. Redirects to the client are returned.
 * Cookies are kept across requests to allow for single sign-on. They are passed on manually, since Keycloak marks them
 * as secure, which Java's cookie handling does not send over plain HTTP.
 */
class BrowserLogin {

    static final String LOGIN_FORM = "kc-form-login";
    static final String UPDATE_PASSWORD_FORM = "kc-passwd-update-form";

    private static final Pattern FORM_ACTION = Pattern.compile("action=\"([^\"]+)\"");
    private static final Pattern CODE = Pattern.compile("[?&]code=([^&#]+)");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private final Map<String, String> cookies = new LinkedHashMap<>();

    private final String serverUrl;

    BrowserLogin(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    String authorizationCode(String client, String redirectUri, String username, String password)
        throws IOException, InterruptedException {
        return code(login(authorize(client, redirectUri), username, password));
    }

    HttpResponse<String> authorize(String client, String redirectUri) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/realms/" + REALM_TEST + "/protocol/openid-connect/auth"
                + "?response_type=code&scope=openid&state=state"
                + "&client_id=" + encode(client)
                + "&redirect_uri=" + encode(redirectUri)))
            .GET());
    }

    HttpResponse<String> login(HttpResponse<String> loginPage, String username, String password)
        throws IOException, InterruptedException {
        return submitForm(loginPage, LOGIN_FORM,
            Map.of("username", username, "password", password, "credentialId", ""));
    }

    HttpResponse<String> submitForm(HttpResponse<String> page, String formId, Map<String, String> fields)
        throws IOException, InterruptedException {
        String form = fields.entrySet().stream()
            .map(it -> it.getKey() + "=" + encode(it.getValue()))
            .collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder()
            .uri(URI.create(formAction(page.body(), formId)))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)));
    }

    static String code(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(302);
        Matcher code = CODE.matcher(response.headers().firstValue("Location").orElseThrow());
        assertThat(code.find()).isTrue();
        return code.group(1);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        HttpResponse<String> response = sendWithCookies(request);
        String location = response.headers().firstValue("Location").orElse("");
        if (response.statusCode() == 302 && location.startsWith(serverUrl)) {
            return send(HttpRequest.newBuilder().uri(URI.create(location)).GET());
        }
        return response;
    }

    private HttpResponse<String> sendWithCookies(HttpRequest.Builder request)
        throws IOException, InterruptedException {
        if (!cookies.isEmpty()) {
            request.header("Cookie", cookies.entrySet().stream()
                .map(it -> it.getKey() + "=" + it.getValue())
                .collect(Collectors.joining("; ")));
        }
        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        response.headers().allValues("Set-Cookie").forEach(this::storeCookie);
        return response;
    }

    private void storeCookie(String setCookie) {
        String[] nameAndValue = setCookie.split(";", 2)[0].split("=", 2);
        if (setCookie.contains("Max-Age=0")) {
            cookies.remove(nameAndValue[0]);
        } else {
            cookies.put(nameAndValue[0], nameAndValue[1]);
        }
    }

    private static String formAction(String page, String formId) {
        Matcher form = Pattern.compile("<form[^>]*id=\"" + formId + "\"[^>]*>").matcher(page);
        assertThat(form.find()).as(page).isTrue();
        Matcher action = FORM_ACTION.matcher(form.group());
        assertThat(action.find()).as(form.group()).isTrue();
        return action.group(1).replace("&amp;", "&");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
