package de.sventorben.keycloak.authentication;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.ws.rs.NotAuthorizedException;
import java.time.Duration;

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
    private static final String CLIENT_TEST_UNRESTRICTED = "test-client-unrestricted";
    private static final String USER_TEST_RESTRICTED = "test-restricted";
    private static final String PASS_TEST_RESTRICTED = "test";
    private static final String USER_TEST_UNRESTRICTED = "test-unrestricted";
    private static final String PASS_TEST_UNRESTRICTED = "test";

    private static String KEYCLOAK_AUTH_URL;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = new CustomKeycloakContainer(
            "quay.io/keycloak/keycloak:" + System.getProperty("version.keycloak", "latest"))
            .withProviderClassesFrom("target/classes")
            .withAdminUsername(KEYCLOAK_ADMIN_USER)
            .withAdminPassword(KEYCLOAK_ADMIN_PASS)
            .withRealmImportFiles("master-realm.json", "test-realm.json")
            .withExposedPorts(KEYCLOAK_HTTP_PORT)
            .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
            .waitingFor(Wait.forHttp("/auth/")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(1)));


    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
    }

    @Nested
    class RestrictedClient {

        @Test
        void accessForUserWithoutRoleIsDenied() {
            Keycloak keycloak = keycloak(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_RESTRICTED);
            assertThatThrownBy(() -> keycloak.tokenManager().getAccessToken())
                    .isInstanceOf(NotAuthorizedException.class);
        }

        @Test
        void accessForUserWithRoleIsAllowed() {
            Keycloak keycloak = keycloak(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED);
            assertThat(keycloak.tokenManager().getAccessToken()).isNotNull();
        }
    }

    @Nested
    class UnrestrictedClient {

        @Test
        void accessForRestrictedUserIsAllowed() {
            Keycloak keycloak = keycloak(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_UNRESTRICTED);
            assertThat(keycloak.tokenManager().getAccessToken()).isNotNull();
        }

        @Test
        void accessForUnrestrictedUserIsAllowed() {
            Keycloak keycloak = keycloak(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_UNRESTRICTED);
            assertThat(keycloak.tokenManager().getAccessToken()).isNotNull();
        }
    }

    private static Keycloak keycloak(String username, String password, String client) {
        return Keycloak.getInstance(KEYCLOAK_AUTH_URL, REALM_TEST, username, password, client);
    }

}
