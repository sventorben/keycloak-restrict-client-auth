package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The client restricted by policy denies access if its permission is missing, no matter which policy enforcement mode
 * is configured. Keycloak itself permits all requests in mode DISABLED, and requests to resources without permissions
 * in mode PERMISSIVE.
 */
@Testcontainers
class PolicyEnforcementModeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyEnforcementModeIT.class);

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
    }

    @ParameterizedTest
    @EnumSource(PolicyEnforcementMode.class)
    void accessIsDeniedIfPermissionIsMissing(PolicyEnforcementMode policyEnforcementMode) {
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            ClientResourcePermission permission = new ClientResourcePermission(admin);
            permission.removePermission();
            permission.setPolicyEnforcementMode(policyEnforcementMode);
            try (Keycloak keycloak = TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, USER_TEST_UNRESTRICTED,
                PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED_BY_POLICY, CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY)) {
                assertThatThrownBy(() -> keycloak.tokenManager().grantToken())
                    .isInstanceOf(BadRequestException.class);
            } finally {
                permission.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
                permission.restorePermission();
            }
        }
    }
}
