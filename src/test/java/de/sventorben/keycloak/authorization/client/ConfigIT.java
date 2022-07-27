package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static de.sventorben.keycloak.authorization.client.TestConstants.KEYCLOAK_HTTP_PORT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@Testcontainers
class ConfigIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigIT.class);

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
    void assumeQuarkus() {
        assumeThat(FullImageName.getDistribution())
            .withFailMessage("Test only supported for quarkus-based distribution ")
            .isEqualTo(FullImageName.Distribution.quarkus);
    }

    @Test
    @DisplayName("Client Role Name can be configured via SPI config")
    void clientRoleName() {
        String clientRoleName;
        try(Keycloak keycloakAdminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient()) {
            clientRoleName = keycloakAdminClient
                .serverInfo()
                .getInfo()
                .getProviders()
                .get("restrict-client-auth-access-provider")
                .getProviders()
                .get("client-role")
                .getOperationalInfo()
                .get("clientRoleName");
        }
        assertThat(clientRoleName).isEqualTo("band and crew only");
    }

}
