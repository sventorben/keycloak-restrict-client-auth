package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.util.JsonSerialization;
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

/**
 * The service account of the client restricted by policy obtains tokens with restricted clients in their audience.
 * Service accounts are checked, so the service account needs access to the clients in the audience, except the client
 * itself.
 */
@Testcontainers
class ServiceAccountIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceAccountIT.class);

    private static final String AUDIENCE_MAPPER = "audience ";

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
            EnforceAccessPolicy.enable(admin.realm(REALM_TEST), Map.of("checkServiceAccounts", true));
        }
    }

    @AfterEach
    void removeAudienceAndAccess() {
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            RealmResource realm = admin.realm(REALM_TEST);
            ClientResource serviceAccountClient = client(realm, CLIENT_TEST_RESTRICTED_BY_POLICY);
            serviceAccountClient.getProtocolMappers().getMappers().stream()
                .filter(it -> it.getName().startsWith(AUDIENCE_MAPPER))
                .forEach(it -> serviceAccountClient.getProtocolMappers().delete(it.getId()));
            RoleScopeResource clientRoles = serviceAccountClientRoles(realm);
            clientRoles.remove(clientRoles.listAll());
        }
    }

    @Test
    void tokenIsIssuedForClientItselfWithoutAccess() throws Exception {
        addToAudience(CLIENT_TEST_RESTRICTED_BY_POLICY);

        HttpResponse<String> response = TOKEN_ENDPOINT.clientCredentials(CLIENT_TEST_RESTRICTED_BY_POLICY,
            CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY);

        assertGranted(response);
        String accessToken = JsonSerialization.readValue(response.body(), AccessTokenResponse.class).getToken();
        assertThat(new JWSInput(accessToken).readJsonContent(AccessToken.class).getAudience())
            .contains(CLIENT_TEST_RESTRICTED_BY_POLICY);
    }

    @Test
    void tokenIsDeniedIfServiceAccountHasNoAccessToClientInAudience() throws Exception {
        addToAudience(CLIENT_TEST_RESTRICTED);

        HttpResponse<String> response = TOKEN_ENDPOINT.clientCredentials(CLIENT_TEST_RESTRICTED_BY_POLICY,
            CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("\"unauthorized_client\"", "Access to client is denied.");
    }

    @Test
    void tokenIsIssuedIfServiceAccountHasAccessToClientInAudience() throws Exception {
        addToAudience(CLIENT_TEST_RESTRICTED);
        grantServiceAccountAccessToRestrictedClient();

        assertGranted(TOKEN_ENDPOINT.clientCredentials(CLIENT_TEST_RESTRICTED_BY_POLICY,
            CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY));
    }

    private static void assertGranted(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private static void addToAudience(String clientId) {
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(AUDIENCE_MAPPER + clientId);
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-audience-mapper");
            mapper.setConfig(Map.of("included.client.audience", clientId, "access.token.claim", "true"));
            client(admin.realm(REALM_TEST), CLIENT_TEST_RESTRICTED_BY_POLICY).getProtocolMappers()
                .createMapper(mapper).close();
        }
    }

    private static void grantServiceAccountAccessToRestrictedClient() {
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            RealmResource realm = admin.realm(REALM_TEST);
            serviceAccountClientRoles(realm).add(List.of(client(realm, CLIENT_TEST_RESTRICTED)
                .roles().get("restricted-access").toRepresentation()));
        }
    }

    private static RoleScopeResource serviceAccountClientRoles(RealmResource realm) {
        String serviceAccountId = client(realm, CLIENT_TEST_RESTRICTED_BY_POLICY).getServiceAccountUser().getId();
        String restrictedClientId = realm.clients().findByClientId(CLIENT_TEST_RESTRICTED).get(0).getId();
        return realm.users().get(serviceAccountId).roles().clientLevel(restrictedClientId);
    }

    private static ClientResource client(RealmResource realm, String clientId) {
        ClientRepresentation client = realm.clients().findByClientId(clientId).get(0);
        return realm.clients().get(client.getId());
    }
}
