package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class ClientPolicyIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPolicyIT.class);

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
    @CsvSource(value = {"test-client-restricted,true", "test-client-restricted-by-policy,true", "test-client-unrestricted,false"})
    void checkIfRestrictedAccessEnabledConditionWorks(String clientName, boolean expectedConsent) {
        try(Keycloak admin = keycloakAdmin()) {
            ClientRepresentation client = admin.realm(REALM_TEST).clients().findByClientId(clientName).get(0);
            assertThat(client.isConsentRequired()).isFalse();

            // update client to trigger policy
            ClientResource clientResource = admin.realm(REALM_TEST).clients().get(client.getId());
            client.setName(UUID.randomUUID().toString());
            clientResource.update(client);

            client = clientResource.toRepresentation();
            assertThat(client.isConsentRequired()).isEqualTo(expectedConsent);
        }
    }

    @Test
    void checkIfRestrictedAccessAutoConfigWorks() {
        try(Keycloak keycloakAdmin = keycloakAdmin()) {

            try(Keycloak keycloakTest = keycloakTest(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
                CLIENT_TEST_UNRESTRICTED)) {
                assertThat(keycloakTest.tokenManager().grantToken()).isNotNull();

                // enable the policy
                RealmResource testRealm = keycloakAdmin.realm(REALM_TEST);
                ClientPoliciesRepresentation policies = testRealm.clientPoliciesPoliciesResource().getPolicies();
                ClientPolicyRepresentation policy = policies.getPolicies().stream()
                    .filter(it -> "enable-restricted-access-all-clients" .equals(it.getName()))
                    .findFirst()
                    .get();
                policy.setEnabled(true);
                testRealm.clientPoliciesPoliciesResource().updatePolicies(policies);

                // update the client to trigger policy
                ClientRepresentation client = testRealm.clients().findByClientId(CLIENT_TEST_UNRESTRICTED).get(0);
                ClientResource clientResource = testRealm.clients().get(client.getId());
                client.setName(UUID.randomUUID().toString());
                clientResource.update(client);

                assertThatThrownBy(() -> keycloakTest.tokenManager().grantToken())
                    .isInstanceOf(BadRequestException.class);
            }
        }
    }

    @Test
    void restrictedAccessAutoConfigRestrictsNewClients() {
        try (Keycloak keycloakAdmin = keycloakAdmin()) {
            RealmResource testRealm = keycloakAdmin.realm(REALM_TEST);
            ClientPoliciesRepresentation policies = testRealm.clientPoliciesPoliciesResource().getPolicies();
            policies.getPolicies().stream()
                .filter(it -> "enable-restricted-access-all-clients".equals(it.getName()))
                .forEach(it -> it.setEnabled(true));
            testRealm.clientPoliciesPoliciesResource().updatePolicies(policies);

            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("new-client-" + UUID.randomUUID());
            client.setPublicClient(true);
            try (Response response = testRealm.clients().create(client)) {
                assertThat(response.getStatus()).isEqualTo(201);
            }

            String id = testRealm.clients().findByClientId(client.getClientId()).get(0).getId();
            assertThat(testRealm.clients().get(id).roles().list())
                .extracting(RoleRepresentation::getName)
                .contains("restricted-access");
        }
    }

    private static Keycloak keycloakAdmin() {
        return Keycloak.getInstance(KEYCLOAK_AUTH_URL, "master", KEYCLOAK_ADMIN_USER, KEYCLOAK_ADMIN_PASS, "admin-cli");
    }

    private static Keycloak keycloakTest(String username, String password, String client) {
        return TestConstants.keycloakTest(KEYCLOAK_AUTH_URL, username, password, client);
    }

}
