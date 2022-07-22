package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static de.sventorben.keycloak.authorization.client.TestConstants.KEYCLOAK_ADMIN_PASS;
import static de.sventorben.keycloak.authorization.client.TestConstants.KEYCLOAK_ADMIN_USER;
import static de.sventorben.keycloak.authorization.client.TestConstants.KEYCLOAK_HTTP_PORT;
import static de.sventorben.keycloak.authorization.client.TestConstants.REALM_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@Testcontainers
class ClientPolicyIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPolicyIT.class);

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

    @BeforeEach
    void assumeQuarkusIfNightlyBuild() {
        if (FullImageName.isNightlyVersion()) {
            assumeThat(FullImageName.getDistribution())
                .withFailMessage("Nightly build only supported for quarkus-based distribution ")
                .isEqualTo(FullImageName.Distribution.quarkus);
        }
    }

    @ParameterizedTest
    @CsvSource(value = {"test-client-restricted,true", "test-client-restricted-by-policy,true", "test-client-unrestricted,false"})
    void checkIfRestrictedAccessEnabledConditionWorks(String clientName, boolean expectedConsent) {
        Keycloak admin = keycloakAdmin();
        ClientRepresentation client = admin.realm(REALM_TEST).clients().findByClientId(clientName).get(0);
        assertThat(client.isConsentRequired()).isFalse();

        ClientResource clientResource = admin.realm(REALM_TEST).clients().get(client.getId());
        client.setName(UUID.randomUUID().toString());
        clientResource.update(client);

        client = clientResource.toRepresentation();
        assertThat(client.isConsentRequired()).isEqualTo(expectedConsent);
    }

    private static Keycloak keycloakAdmin() {
        return Keycloak.getInstance(KEYCLOAK_AUTH_URL, "master", KEYCLOAK_ADMIN_USER, KEYCLOAK_ADMIN_PASS, "admin-cli");
    }

}
