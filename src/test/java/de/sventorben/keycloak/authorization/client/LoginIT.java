package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class LoginIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginIT.class);

    private static String KEYCLOAK_AUTH_URL;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withProviderClassesFrom("target/classes")
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/test-realm.json")
        .withStartupTimeout(Duration.ofSeconds(90));

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
    }

    /**
     * If no access provider is configured for the authenticator, and no server-wide default access provider is configured via
     * SPI configuration, then we fallback to 'client-role'.
     */
    @Nested
    class RestrictedClient {

        @ParameterizedTest
        @CsvSource(value = {"client-role", "null"}, nullValues = "null")
        void accessForUserWithoutRoleIsDenied(String accessProviderId) {
            LoginIT.this.switchAccessProvider(accessProviderId);
            try (Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_RESTRICTED)) {
                assertThatThrownBy(() -> keycloak.tokenManager().grantToken())
                    .isInstanceOf(NotAuthorizedException.class);
            }
        }

        @ParameterizedTest
        @CsvSource(value = {"client-role", "null"}, nullValues = "null")
        void accessForUserWithRoleIsAllowed(String accessProviderId) {
            LoginIT.this.switchAccessProvider(accessProviderId);
            try (Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED)) {
                assertThat(keycloak.tokenManager().grantToken()).isNotNull();
            }
        }
    }

    @Nested
    class RestrictedClientByPolicy {

        @BeforeEach
        void switchAccessProvider() {
            LoginIT.this.switchAccessProvider("policy");
        }

        @Test
        void accessForUserWithoutRoleIsDenied() {
            try (Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
                CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY)) {
                assertThatThrownBy(() -> keycloak.tokenManager().grantToken())
                    .isInstanceOf(NotAuthorizedException.class);
            }
        }

        @Test
        void accessForUserWithRoleIsAllowed() {
            try (Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
                CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY)) {
                assertThat(keycloak.tokenManager().grantToken()).isNotNull();
            }
        }
    }

    @Nested
    class UnrestrictedClient {

        @BeforeEach
        void switchAccessProvider() {
            LoginIT.this.switchAccessProvider(null);
        }

        @Test
        void accessForRestrictedUserIsAllowed() {
            try (Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_UNRESTRICTED)) {
                assertThat(keycloak.tokenManager().grantToken()).isNotNull();
            }
        }

        @Test
        void accessForUnrestrictedUserIsAllowed() {
            try (Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_UNRESTRICTED)) {
                assertThat(keycloak.tokenManager().grantToken()).isNotNull();
            }
        }
    }


    private void switchAccessProvider(String accessProviderId) {
        try (Keycloak admin = keycloakAdmin()) {
            AuthenticationManagementResource flows = admin.realm(REALM_TEST).flows();
            String authenticationConfigId = flows
                .getExecutions("direct-grant-restrict-client-auth").stream()
                .filter(it -> it.getProviderId().equalsIgnoreCase("restrict-client-auth-authenticator"))
                .findFirst()
                .get()
                .getAuthenticationConfig();
            AuthenticatorConfigRepresentation authenticatorConfig = flows.getAuthenticatorConfig(
                authenticationConfigId);
            Map<String, String> config = authenticatorConfig.getConfig();
            if (accessProviderId == null) {
                config.remove("accessProviderId");
            } else {
                config.put("accessProviderId", accessProviderId);
            }
            authenticatorConfig.setConfig(config);
            flows.updateAuthenticatorConfig(authenticationConfigId, authenticatorConfig);
        }
    }

    private static Keycloak keycloakAdmin() {
        return TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL);
    }

    private static Keycloak keycloakTest(String username, String password, String client) {
        return TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, username, password, client, null);
    }

    private static Keycloak keycloakTest(String username, String password, String client, String clientSecret) {
        return TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, username, password, client, clientSecret);
    }

}
