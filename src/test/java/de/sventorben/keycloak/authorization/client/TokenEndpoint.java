package de.sventorben.keycloak.authorization.client;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static de.sventorben.keycloak.authorization.client.TestConstants.REALM_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Talks to the token endpoint directly, since the admin client hides failed refreshes and logs out on close.
 */
class TokenEndpoint {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String serverUrl;

    TokenEndpoint(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    AccessTokenResponse grantToken(String username, String password, String client, String scope)
        throws IOException, InterruptedException {
        HttpResponse<String> response = passwordGrant(username, password, client, scope);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JsonSerialization.readValue(response.body(), AccessTokenResponse.class);
    }

    HttpResponse<String> passwordGrant(String username, String password, String client, String scope)
        throws IOException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "password");
        params.put("client_id", client);
        params.put("username", username);
        params.put("password", password);
        params.put("scope", scope);
        return post(params);
    }

    HttpResponse<String> exchangeCode(String client, String code, String redirectUri)
        throws IOException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", client);
        params.put("code", code);
        params.put("redirect_uri", redirectUri);
        return post(params);
    }

    HttpResponse<String> refresh(String client, String refreshToken) throws IOException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("client_id", client);
        params.put("refresh_token", refreshToken);
        return post(params);
    }

    HttpResponse<String> exchange(String client, String clientSecret, String subjectToken, String audience)
        throws IOException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.put("client_id", client);
        params.put("client_secret", clientSecret);
        params.put("subject_token", subjectToken);
        params.put("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.put("audience", audience);
        return post(params);
    }

    private HttpResponse<String> post(Map<String, String> params) throws IOException, InterruptedException {
        String form = params.entrySet().stream()
            .filter(it -> it.getValue() != null)
            .map(it -> it.getKey() + "=" + URLEncoder.encode(it.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/realms/" + REALM_TEST + "/protocol/openid-connect/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
