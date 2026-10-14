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

import java.lang.module.ModuleDescriptor.Version;
import java.net.http.HttpResponse;
import java.time.Duration;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Emergency procedure for lockouts: the realm has the required action registered and enabled, but the required action
 * provider is disabled via the server configuration. Keycloak then skips the required action, so users can log in
 * again.
 */
@Testcontainers
class EmergencyIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmergencyIT.class);

    private static final String REALM_EMERGENCY = "emergency";

    private static String KEYCLOAK_AUTH_URL;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/emergency-realm.json")
        .withStartupTimeout(Duration.ofSeconds(90))
        .withClasspathResourceMapping(serverConfiguration(), "/opt/keycloak/conf/keycloak.conf", BindMode.READ_ONLY);

    /**
     * Keycloak 26.2 does not support the new format of SPI options with '--' separators.
     */
    private static String serverConfiguration() {
        if (FullImageName.isLatestVersion() || FullImageName.isNightlyVersion()
            || FullImageName.getParsedVersion().compareTo(Version.parse("26.3")) >= 0) {
            return "keycloak-required-action-disabled.conf";
        }
        return "keycloak-required-action-disabled-legacy.conf";
    }

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
    }

    @Test
    void requiredActionIsRegisteredAndEnabledInRealm() {
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            assertThat(admin.realm(REALM_EMERGENCY).flows().getRequiredActions())
                .anySatisfy(it -> {
                    assertThat(it.getAlias()).isEqualTo(RequiredActions.RESTRICT_CLIENT_AUTH);
                    assertThat(it.isEnabled()).isTrue();
                });
        }
    }

    @Test
    void userWithoutAccessCanLogInSinceRequiredActionProviderIsDisabled() throws Exception {
        HttpResponse<String> response = new TokenEndpoint(KEYCLOAK_AUTH_URL, REALM_EMERGENCY)
            .passwordGrant(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_RESTRICTED, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

}
