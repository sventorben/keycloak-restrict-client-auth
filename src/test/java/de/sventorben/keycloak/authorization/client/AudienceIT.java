package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tokens for the unrestricted client contain the restricted client in their audience.
 */
@Testcontainers
class AudienceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudienceIT.class);

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
            EnforceAccessPolicy.enable(testRealm, Map.of("accessProviderId", "client-role", "checkAudience", true));
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("audience " + CLIENT_TEST_RESTRICTED);
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-audience-mapper");
            mapper.setConfig(Map.of(
                "included.client.audience", CLIENT_TEST_RESTRICTED,
                "access.token.claim", "true"));
            String clientId = testRealm.clients().findByClientId(CLIENT_TEST_UNRESTRICTED).get(0).getId();
            testRealm.clients().get(clientId).getProtocolMappers().createMapper(mapper).close();
        }
    }

    @Test
    void tokenForUserWithAccessToClientInAudienceIsAllowed() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.passwordGrant(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_TEST_UNRESTRICTED, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        String accessToken = JsonSerialization.readValue(response.body(), AccessTokenResponse.class).getToken();
        assertThat(new JWSInput(accessToken).readJsonContent(AccessToken.class).getAudience())
            .contains(CLIENT_TEST_RESTRICTED);
    }

    @Test
    void tokenForUserWithoutAccessToClientInAudienceIsDenied() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.passwordGrant(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_TEST_UNRESTRICTED, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("invalid_grant", "Access to client is denied.");
    }

}
