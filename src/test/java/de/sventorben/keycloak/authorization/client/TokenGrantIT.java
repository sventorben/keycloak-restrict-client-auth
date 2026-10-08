package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.time.Duration;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test realm uses Keycloak's built-in browser flow and, in this test, also Keycloak's built-in direct grant flow.
 * Hence, only the client policy executor restricts access.
 */
@Testcontainers
class TokenGrantIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenGrantIT.class);

    private static final String REDIRECT_URI = "http://localhost/callback";

    private static String KEYCLOAK_AUTH_URL;
    private static TokenEndpoint TOKEN_ENDPOINT;

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
            RealmResource testRealm = admin.realm(REALM_TEST);
            EnforceAccessPolicy.enable(testRealm);
            RealmRepresentation realm = testRealm.toRepresentation();
            realm.setDirectGrantFlow("direct grant");
            testRealm.update(realm);
        }
    }

    @Test
    void codeExchangeForUserWithAccessIsAllowed() throws Exception {
        HttpResponse<String> response = exchangeCode(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    @Test
    void codeExchangeForUserWithoutAccessIsDenied() throws Exception {
        HttpResponse<String> response = exchangeCode(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED);

        assertDenied(response);
    }

    @Test
    void passwordGrantForUserWithAccessIsAllowed() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.passwordGrant(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_TEST_RESTRICTED, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    @Test
    void passwordGrantForUserWithoutAccessIsDenied() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.passwordGrant(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_TEST_RESTRICTED, null);

        assertDenied(response);
    }

    private static HttpResponse<String> exchangeCode(String username, String password) throws Exception {
        String code = new BrowserLogin(KEYCLOAK_AUTH_URL)
            .authorizationCode(CLIENT_TEST_RESTRICTED, REDIRECT_URI, username, password);
        return TOKEN_ENDPOINT.exchangeCode(CLIENT_TEST_RESTRICTED, code, REDIRECT_URI);
    }

    private static void assertDenied(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("invalid_grant", "Access to client is denied.");
    }

}
