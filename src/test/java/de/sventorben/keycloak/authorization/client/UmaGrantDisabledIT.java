package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.time.Duration;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The extension's implementation of the UMA grant is disabled via the server configuration, so Keycloak uses its own
 * implementation, which does not check access to the restricted resource server.
 */
@Testcontainers
class UmaGrantDisabledIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmaGrantDisabledIT.class);

    private static String KEYCLOAK_AUTH_URL;
    private static TokenEndpoint TOKEN_ENDPOINT;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/test-realm-realm.json")
        .withStartupTimeout(Duration.ofSeconds(90))
        .withClasspathResourceMapping(serverConfiguration(), "/opt/keycloak/conf/keycloak.conf", BindMode.READ_ONLY);

    /**
     * Keycloak 26.2 does not support the new format of SPI options with '--' separators.
     */
    private static String serverConfiguration() {
        if (FullImageName.isAtLeastVersion("26.3")) {
            return "keycloak-uma-grant-disabled.conf";
        }
        return "keycloak-uma-grant-disabled-legacy.conf";
    }

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        TOKEN_ENDPOINT = new TokenEndpoint(KEYCLOAK_AUTH_URL);
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            ResourceForAllUsers.add(admin.realm(REALM_TEST));
        }
    }

    @Test
    void keycloakIssuesRequestingPartyTokenForUserWithoutAccess() throws Exception {
        String accessToken = TOKEN_ENDPOINT.grantToken(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_TEST_UNRESTRICTED, null).getToken();

        HttpResponse<String> response = TOKEN_ENDPOINT.requestingPartyToken(accessToken,
            CLIENT_TEST_RESTRICTED_BY_POLICY, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }
}
