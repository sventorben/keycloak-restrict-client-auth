package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@Testcontainers
class ConfigIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigIT.class);

    private static final int KEYCLOAK_HTTP_PORT = 8080;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withProviderClassesFrom("target/classes")
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withStartupTimeout(Duration.ofSeconds(90))
        .withClasspathResourceMapping("keycloak.conf", "/opt/keycloak/conf/keycloak.conf", BindMode.READ_ONLY);

    @BeforeAll
    static void setUp() {
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
    }

    @BeforeEach
    void assumeQuarkusAbove17() {
        assumeThat(FullImageName.getDistribution())
            .withFailMessage("Test only supported for quarkus-based distribution ")
            .isEqualTo(FullImageName.Distribution.quarkus);
        assumeThat(FullImageName.isLatestVersion() || FullImageName.getParsedVersion().compareTo(
            Version.parse("17")) >= 0)
            .withFailMessage("Test only supported for Keycloak versions >= 17")
            .isTrue();
    }

    @Test
    @DisplayName("Client Role Name can be configured via SPI config")
    void clientRoleName() {
        String clientRoleName = KEYCLOAK_CONTAINER.getKeycloakAdminClient()
            .serverInfo()
            .getInfo()
            .getProviders()
            .get("restrict-client-auth-access-provider")
            .getProviders()
            .get("client-role")
            .getOperationalInfo()
            .get("clientRoleName");
        assertThat(clientRoleName).isEqualTo("band and crew only");
    }

}
