package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
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

import javax.ws.rs.NotAuthorizedException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class LoginIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginIT.class);

    private static final int KEYCLOAK_HTTP_PORT = 8080;

    private static final String KEYCLOAK_ADMIN_PASS = "admin";
    private static final String KEYCLOAK_ADMIN_USER = "admin";

    private static final String REALM_TEST = "test-realm";
    private static final String CLIENT_TEST_RESTRICTED = "test-client-restricted";
    private static final String CLIENT_TEST_RESTRICTED_BY_POLICY = "test-client-restricted-by-policy";
    private static final String CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY = "42437f49-2b56-498e-a67c-13d4ee2d8cad";
    private static final String CLIENT_TEST_UNRESTRICTED = "test-client-unrestricted";
    private static final String USER_TEST_RESTRICTED = "test-restricted";
    private static final String PASS_TEST_RESTRICTED = "test";
    private static final String USER_TEST_UNRESTRICTED = "test-unrestricted";
    private static final String PASS_TEST_UNRESTRICTED = "test";

    private static String KEYCLOAK_AUTH_URL;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = createContainer(
        System.getProperty("keycloak.dist", "keycloak-x"), System.getProperty("keycloak.version", "latest"))
        .withProviderClassesFrom("target/classes")
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/test-realm.json")
        .withStartupTimeout(Duration.ofSeconds(30));

    private static KeycloakContainer createContainer(String dist, String version) {
        if ("keycloak".equalsIgnoreCase(dist) && "latest".equalsIgnoreCase(version)) {
            version = "16.1.1";
        }
        String fullImage = "quay.io/keycloak/" + dist + ":" + version;
        if ("keycloak-x".equalsIgnoreCase(dist) &&
            !"latest".equalsIgnoreCase(version) && Version.parse(version).compareTo(Version.parse("17")) >= 0) {
            fullImage = fullImage.replace("keycloak-x", "keycloak");
        }
        if ("keycloak".equalsIgnoreCase(dist) &&
            !"latest".equalsIgnoreCase(version) && Version.parse(version).compareTo(Version.parse("17")) >= 0) {
            fullImage = fullImage + "-legacy";
        }
        LOGGER.info("Running test with Keycloak image: " + fullImage);
        if ("keycloak-x".equalsIgnoreCase(dist) &&
            !"latest".equalsIgnoreCase(version) && Version.parse(version).compareTo(Version.parse("15.1")) < 0) {
            return new KeycloakXContainer(fullImage);
        }
        return new KeycloakContainer(fullImage);
    }

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
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
            Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_RESTRICTED);
            assertThatThrownBy(() -> keycloak.tokenManager().getAccessToken())
                .isInstanceOf(NotAuthorizedException.class);
        }

        @ParameterizedTest
        @CsvSource(value = {"client-role", "null"}, nullValues = "null")
        void accessForUserWithRoleIsAllowed(String accessProviderId) {
            LoginIT.this.switchAccessProvider(accessProviderId);
            Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED);
            assertThat(keycloak.tokenManager().getAccessToken()).isNotNull();
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
            Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
                CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY);
            assertThatThrownBy(() -> keycloak.tokenManager().getAccessToken())
                .isInstanceOf(NotAuthorizedException.class);
        }

        @Test
        void accessForUserWithRoleIsAllowed() {
            Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
                CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY);
            assertThat(keycloak.tokenManager().getAccessToken()).isNotNull();
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
            Keycloak keycloak = keycloakTest(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_UNRESTRICTED);
            assertThat(keycloak.tokenManager().getAccessToken()).isNotNull();
        }

        @Test
        void accessForUnrestrictedUserIsAllowed() {
            Keycloak keycloak = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_UNRESTRICTED);
            assertThat(keycloak.tokenManager().getAccessToken()).isNotNull();
        }
    }


    private void switchAccessProvider(String accessProviderId) {
        Keycloak admin = keycloakAdmin();
        AuthenticationManagementResource flows = admin.realm(REALM_TEST).flows();
        String authenticationConfigId = flows
            .getExecutions("direct-grant-restricted-client-auth").stream()
            .filter(it -> it.getProviderId().equalsIgnoreCase("restrict-client-auth-authenticator"))
            .findFirst()
            .get()
            .getAuthenticationConfig();
        AuthenticatorConfigRepresentation authenticatorConfig = flows.getAuthenticatorConfig(authenticationConfigId);
        Map<String, String> config = authenticatorConfig.getConfig();
        if (accessProviderId == null) {
            config.remove("accessProviderId");
        } else {
            config.put("accessProviderId", accessProviderId);
        }
        authenticatorConfig.setConfig(config);
        flows.updateAuthenticatorConfig(authenticationConfigId, authenticatorConfig);
    }

    private static Keycloak keycloakAdmin() {
        return keycloak("master", KEYCLOAK_ADMIN_USER, KEYCLOAK_ADMIN_PASS, "admin-cli", null);
    }

    private static Keycloak keycloakTest(String username, String password, String client) {
        return keycloakTest(username, password, client, null);
    }

    private static Keycloak keycloakTest(String username, String password, String client, String clientSecret) {
        return keycloak(REALM_TEST, username, password, client, clientSecret);
    }

    private static Keycloak keycloak(String realm, String username, String password, String client, String clientSecret) {
        return Keycloak.getInstance(KEYCLOAK_AUTH_URL, realm, username, password, client, clientSecret);
    }

}
