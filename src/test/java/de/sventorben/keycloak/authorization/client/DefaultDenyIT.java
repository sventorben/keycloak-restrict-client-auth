package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The realm settings, configured via the disabled required action, deny access by default. Users with the default
 * access role may access clients without the client role. The authenticator in the direct grant flow of the test realm
 * and the client policy executor both apply these settings.
 */
@Testcontainers
class DefaultDenyIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDenyIT.class);

    private static final String REQUIRED_ACTION = "restrict-client-auth-enforce";
    private static final String DEFAULT_ACCESS_ROLE = "default-access";
    private static final String CLIENT_WITHOUT_AUTHENTICATOR = "test-client-without-authenticator";

    private static String KEYCLOAK_AUTH_URL;
    private static TokenEndpoint TOKEN_ENDPOINT;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/test-realm-realm.json")
        .withStartupTimeout(Duration.ofSeconds(90));

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        TOKEN_ENDPOINT = new TokenEndpoint(KEYCLOAK_AUTH_URL);
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            RealmResource testRealm = admin.realm(REALM_TEST);
            createDefaultAccessRole(testRealm);
            createClientWithoutAuthenticator(testRealm);
            EnforceAccessPolicy.enable(testRealm);
            registerDisabledRequiredAction(testRealm.flows());
            updateRealmSettings(testRealm.flows(),
                Map.of("defaultDeny", "true", "defaultAccessRole", DEFAULT_ACCESS_ROLE));
        }
    }

    @Test
    void authenticatorPermitsAccessToUnrestrictedClientForUserWithDefaultAccessRole() throws Exception {
        assertGranted(TOKEN_ENDPOINT.passwordGrant(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_TEST_UNRESTRICTED, null));
    }

    @Test
    void authenticatorDeniesAccessToUnrestrictedClientForUserWithoutDefaultAccessRole() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.passwordGrant(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_TEST_UNRESTRICTED, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(401);
    }

    @Test
    void authenticatorDecidesAccessToRestrictedClientByClientRole() throws Exception {
        assertGranted(TOKEN_ENDPOINT.passwordGrant(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_TEST_RESTRICTED, null));
    }

    @Test
    void executorPermitsAccessToUnrestrictedClientForUserWithDefaultAccessRole() throws Exception {
        assertGranted(TOKEN_ENDPOINT.passwordGrant(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_WITHOUT_AUTHENTICATOR, null));
    }

    @Test
    void executorDeniesAccessToUnrestrictedClientForUserWithoutDefaultAccessRole() throws Exception {
        HttpResponse<String> response = TOKEN_ENDPOINT.passwordGrant(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_WITHOUT_AUTHENTICATOR, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("invalid_grant", "Access to client is denied.");
    }

    @Test
    void rejectsRealmSettingsWithUnknownAccessProvider() {
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            AuthenticationManagementResource flows = admin.realm(REALM_TEST).flows();

            assertThatThrownBy(() -> updateRealmSettings(flows, Map.of("accessProviderId", "unknown")))
                .isInstanceOf(BadRequestException.class);
        }
    }

    private static void assertGranted(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private static void createDefaultAccessRole(RealmResource testRealm) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(DEFAULT_ACCESS_ROLE);
        testRealm.roles().create(role);
        String userId = testRealm.users().search(USER_TEST_UNRESTRICTED, true).get(0).getId();
        testRealm.users().get(userId).roles().realmLevel()
            .add(List.of(testRealm.roles().get(DEFAULT_ACCESS_ROLE).toRepresentation()));
    }

    /**
     * Uses Keycloak's built-in direct grant flow without the authenticator, so that only the executor restricts access.
     */
    private static void createClientWithoutAuthenticator(RealmResource testRealm) {
        String directGrantFlowId = testRealm.flows().getFlows().stream()
            .filter(it -> "direct grant".equals(it.getAlias()))
            .findFirst()
            .orElseThrow()
            .getId();
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_WITHOUT_AUTHENTICATOR);
        client.setPublicClient(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setAuthenticationFlowBindingOverrides(Map.of("direct_grant", directGrantFlowId));
        testRealm.clients().create(client).close();
    }

    private static void registerDisabledRequiredAction(AuthenticationManagementResource flows) {
        RequiredActionProviderSimpleRepresentation registration = new RequiredActionProviderSimpleRepresentation();
        registration.setProviderId(REQUIRED_ACTION);
        registration.setName("Restrict user authentication on clients");
        flows.registerRequiredAction(registration);
        RequiredActionProviderRepresentation requiredAction = flows.getRequiredAction(REQUIRED_ACTION);
        requiredAction.setEnabled(false);
        flows.updateRequiredAction(REQUIRED_ACTION, requiredAction);
    }

    private static void updateRealmSettings(AuthenticationManagementResource flows, Map<String, String> settings) {
        RequiredActionConfigRepresentation config = new RequiredActionConfigRepresentation();
        config.setConfig(settings);
        flows.updateRequiredActionConfig(REQUIRED_ACTION, config);
    }

}
