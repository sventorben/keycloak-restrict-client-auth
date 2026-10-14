package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The authenticator in the direct grant flow of the test realm uses the 'auto' access provider. It restricts access to
 * the client restricted by client role and to the client restricted by policy with a single configuration.
 */
@Testcontainers
class AutoAccessProviderIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAccessProviderIT.class);

    private static String KEYCLOAK_AUTH_URL;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/test-realm-realm.json")
        .withStartupTimeout(Duration.ofSeconds(90));

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            AuthenticationManagementResource flows = admin.realm(REALM_TEST).flows();
            String configId = flows.getExecutions("direct-grant-restrict-client-auth").stream()
                .filter(it -> "restrict-client-auth-authenticator".equals(it.getProviderId()))
                .findFirst()
                .orElseThrow()
                .getAuthenticationConfig();
            AuthenticatorConfigRepresentation authenticatorConfig = flows.getAuthenticatorConfig(configId);
            Map<String, String> config = new HashMap<>(authenticatorConfig.getConfig());
            config.put("accessProviderId", "auto");
            authenticatorConfig.setConfig(config);
            flows.updateAuthenticatorConfig(configId, authenticatorConfig);
        }
    }

    @Test
    void permitsAccessToClientRestrictedByClientRoleForUserWithClientRole() {
        try (Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED)) {
            assertThat(keycloak.tokenManager().grantToken()).isNotNull();
        }
    }

    @Test
    void deniesAccessToClientRestrictedByClientRoleForUserWithoutClientRole() {
        try (Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_RESTRICTED)) {
            assertThatThrownBy(() -> keycloak.tokenManager().grantToken()).isInstanceOf(BadRequestException.class);
        }
    }

    @Test
    void permitsAccessToClientRestrictedByPolicyForPermittedUser() {
        try (Keycloak keycloak = TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, USER_TEST_UNRESTRICTED,
            PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY)) {
            assertThat(keycloak.tokenManager().grantToken()).isNotNull();
        }
    }

    @Test
    void deniesAccessToClientRestrictedByPolicyForUserWithoutPermission() {
        try (Keycloak keycloak = TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, USER_TEST_RESTRICTED,
            PASS_TEST_RESTRICTED, CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY)) {
            assertThatThrownBy(() -> keycloak.tokenManager().grantToken()).isInstanceOf(BadRequestException.class);
        }
    }

    @Test
    void permitsAccessToUnrestrictedClient() {
        try (Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_UNRESTRICTED)) {
            assertThat(keycloak.tokenManager().grantToken()).isNotNull();
        }
    }

    private static Keycloak keycloakTest(String username, String password, String client) {
        return TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, username, password, client);
    }

}
