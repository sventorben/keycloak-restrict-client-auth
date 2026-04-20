package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.SpiInfoRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the AccountApplicationsFilter.
 * <p>
 * Note: Full end-to-end testing of the /account/applications endpoint filtering
 * requires additional realm configuration (audience mappers, scopes) that is
 * beyond the scope of these basic integration tests. These tests verify the
 * filter provider is properly loaded and configured.
 */
@Testcontainers
class AccountApplicationsFilterIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountApplicationsFilterIT.class);

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
        LOGGER.info("Running test with Keycloak image: {}", FullImageName.get());
    }

    @Test
    void filterProviderIsLoaded() {
        try (Keycloak admin = keycloakAdmin()) {
            // Verify the SPI is registered
            Map<String, SpiInfoRepresentation> providers = admin.serverInfo().getInfo().getProviders();
            assertThat(providers).containsKey("restrict-client-auth-account-filter");

            // Verify the default provider is available
            SpiInfoRepresentation spiInfo = providers.get("restrict-client-auth-account-filter");
            assertThat(spiInfo.getProviders()).containsKey("default");
        }
    }

    @Test
    void filterConfigurationIsCorrect() {
        try (Keycloak admin = keycloakAdmin()) {
            Map<String, SpiInfoRepresentation> providers = admin.serverInfo().getInfo().getProviders();
            SpiInfoRepresentation spiInfo = providers.get("restrict-client-auth-account-filter");
            var defaultProvider = spiInfo.getProviders().get("default");

            // Verify operational info contains the expected config
            Map<String, String> operationalInfo = defaultProvider.getOperationalInfo();

            assertThat(operationalInfo)
                .containsEntry("filterDynamicApps", "true")
                .containsEntry("filterAlwaysDisplayApps", "true");
        }
    }

    private static Keycloak keycloakAdmin() {
        return TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL);
    }
}
