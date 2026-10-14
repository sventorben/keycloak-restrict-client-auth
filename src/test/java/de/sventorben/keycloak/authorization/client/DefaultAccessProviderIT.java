package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The server-wide default access provider is set to 'policy' via the server configuration. The authenticator in the
 * direct grant flow has no access provider configured, so it uses the server-wide default access provider. The
 * required action is disabled to test the authenticator in isolation.
 */
@Testcontainers
class DefaultAccessProviderIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAccessProviderIT.class);

    private static String KEYCLOAK_AUTH_URL;

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
            return "keycloak-default-access-provider.conf";
        }
        return "keycloak-default-access-provider-legacy.conf";
    }

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            RequiredActions.disable(admin.realm(REALM_TEST));
        }
    }

    @Test
    void userWithoutPermissionIsDeniedAccessToClientRestrictedByPolicy() {
        try (Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY)) {
            assertThatThrownBy(() -> keycloak.tokenManager().grantToken())
                .isInstanceOf(BadRequestException.class);
        }
    }

    @Test
    void userWithPermissionIsAllowedAccessToClientRestrictedByPolicy() {
        try (Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY)) {
            assertThat(keycloak.tokenManager().grantToken()).isNotNull();
        }
    }

    @Test
    void clientRoleIsIgnoredSinceServerWideDefaultAccessProviderIsPolicy() {
        try (Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_RESTRICTED)) {
            assertThat(keycloak.tokenManager().grantToken()).isNotNull();
        }
    }

    private static Keycloak keycloakTest(String username, String password, String clientId) {
        return TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, username, password, clientId);
    }

    private static Keycloak keycloakTest(String username, String password, String clientId, String clientSecret) {
        return TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, username, password, clientId, clientSecret);
    }

}
