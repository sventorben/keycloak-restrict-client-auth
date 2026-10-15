package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The client restricted by policy is a resource server with another resource that all users may access. Users without
 * access to the client must not obtain requesting party tokens (UMA grant) for it, neither via the audience nor via a
 * permission ticket.
 */
@Testcontainers
class UmaIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmaIT.class);

    private static String KEYCLOAK_AUTH_URL;
    private static TokenEndpoint TOKEN_ENDPOINT;
    private static String DATA_RESOURCE_ID;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/test-realm-realm.json")
        .withStartupTimeout(Duration.ofSeconds(90));

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        TOKEN_ENDPOINT = new TokenEndpoint(KEYCLOAK_AUTH_URL);
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            DATA_RESOURCE_ID = ResourceForAllUsers.add(admin.realm(REALM_TEST));
        }
    }

    @Test
    void userWithoutAccessDoesNotObtainRequestingPartyTokenForAudience() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.requestingPartyToken(
            accessToken(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED), CLIENT_TEST_RESTRICTED_BY_POLICY, null);

        assertDenied(response);
    }

    @Test
    void userWithoutAccessDoesNotObtainRequestingPartyTokenForPermissionTicket() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.requestingPartyToken(
            accessToken(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED), null, permissionTicket());

        assertDenied(response);
    }

    @Test
    void userWithAccessObtainsRequestingPartyToken() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.requestingPartyToken(
            accessToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED), CLIENT_TEST_RESTRICTED_BY_POLICY, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(JsonSerialization.readValue(response.body(), AccessTokenResponse.class).getToken()).isNotEmpty();
    }

    private static void assertDenied(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
        assertThat(response.body()).contains("\"access_denied\"", "Access to client is denied.");
    }

    private static String accessToken(String username, String password) throws Exception {
        return TOKEN_ENDPOINT.grantToken(username, password, CLIENT_TEST_UNRESTRICTED, null).getToken();
    }

    /**
     * Requests a permission ticket for the resource that all users may access, as the resource server does.
     */
    private static String permissionTicket() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(KEYCLOAK_AUTH_URL + "/realms/" + REALM_TEST + "/authz/protection/permission"))
            .header("Authorization", "Bearer " + clientCredentialsToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("[{\"resource_id\":\"" + DATA_RESOURCE_ID + "\"}]"))
            .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return (String) JsonSerialization.readValue(response.body(), Map.class).get("ticket");
    }

    private static String clientCredentialsToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(KEYCLOAK_AUTH_URL + "/realms/" + REALM_TEST + "/protocol/openid-connect/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&client_id="
                + CLIENT_TEST_RESTRICTED_BY_POLICY + "&client_secret=" + CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY))
            .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JsonSerialization.readValue(response.body(), AccessTokenResponse.class).getToken();
    }
}
