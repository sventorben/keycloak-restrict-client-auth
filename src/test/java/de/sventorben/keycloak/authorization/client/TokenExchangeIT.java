package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.module.ModuleDescriptor.Version;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keycloak triggers the client policy event for token exchange responses since version 26.8.
 */
@Testcontainers
@EnabledIf("isTokenExchangeResponseEventSupported")
class TokenExchangeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenExchangeIT.class);

    private static final String CLIENT_ROLE_RESTRICTED_ACCESS = "restricted-access";
    private static final String CLIENT_EXCHANGE_UNRESTRICTED = "test-client-exchange-unrestricted";
    private static final String CLIENT_EXCHANGE_RESTRICTED = "test-client-exchange-restricted";
    private static final String CLIENT_SECRET_EXCHANGE = "exchange-secret";

    private static String KEYCLOAK_AUTH_URL;
    private static TokenEndpoint TOKEN_ENDPOINT;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = FullImageName.createContainer()
        .withExposedPorts(KEYCLOAK_HTTP_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
        .withRealmImportFile("/test-realm-realm.json")
        .withStartupTimeout(Duration.ofSeconds(90));

    static boolean isTokenExchangeResponseEventSupported() {
        return FullImageName.isLatestVersion() || FullImageName.isNightlyVersion()
            || FullImageName.getParsedVersion().compareTo(Version.parse("26.8")) >= 0;
    }

    @BeforeAll
    static void setUp() {
        KEYCLOAK_AUTH_URL = KEYCLOAK_CONTAINER.getAuthServerUrl();
        TOKEN_ENDPOINT = new TokenEndpoint(KEYCLOAK_AUTH_URL);
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            RealmResource testRealm = admin.realm(REALM_TEST);
            EnforceAccessPolicy.enable(testRealm);
            createRequester(testRealm, CLIENT_EXCHANGE_UNRESTRICTED);
            createRequester(testRealm, CLIENT_EXCHANGE_RESTRICTED);
            restrictRequester(testRealm);
            ClientResource subjectClient = client(testRealm, CLIENT_TEST_UNRESTRICTED);
            subjectClient.getProtocolMappers().createMapper(audienceMapper(CLIENT_EXCHANGE_UNRESTRICTED)).close();
            subjectClient.getProtocolMappers().createMapper(audienceMapper(CLIENT_EXCHANGE_RESTRICTED)).close();
        }
    }

    @Test
    void exchangeOnUnrestrictedRequesterIsAllowed() throws Exception {
        HttpResponse<String> response = exchange(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_EXCHANGE_UNRESTRICTED, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    @Test
    void exchangeToRestrictedAudienceForUserWithAccessIsAllowed() throws Exception {
        HttpResponse<String> response = exchange(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_EXCHANGE_UNRESTRICTED, CLIENT_TEST_RESTRICTED);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    @Test
    void exchangeToRestrictedAudienceForUserWithoutAccessIsDenied() throws Exception {
        HttpResponse<String> response = exchange(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_EXCHANGE_UNRESTRICTED, CLIENT_TEST_RESTRICTED);

        assertDenied(response);
    }

    @Test
    void exchangeOnRestrictedRequesterForUserWithAccessIsAllowed() throws Exception {
        HttpResponse<String> response = exchange(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_EXCHANGE_RESTRICTED, null);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    @Test
    void exchangeOnRestrictedRequesterForUserWithoutAccessIsDenied() throws Exception {
        HttpResponse<String> response = exchange(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_EXCHANGE_RESTRICTED, null);

        assertDenied(response);
    }

    private static HttpResponse<String> exchange(String username, String password, String requester, String audience)
        throws Exception {
        String subjectToken = TOKEN_ENDPOINT.grantToken(username, password, CLIENT_TEST_UNRESTRICTED, null).getToken();
        return TOKEN_ENDPOINT.exchange(requester, CLIENT_SECRET_EXCHANGE, subjectToken, audience);
    }

    private static void assertDenied(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
        assertThat(response.body()).contains("access_denied", "Access to client is denied.");
    }

    private static void createRequester(RealmResource testRealm, String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setSecret(CLIENT_SECRET_EXCHANGE);
        client.setPublicClient(false);
        client.setStandardFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(false);
        client.setAttributes(Map.of("standard.token.exchange.enabled", "true"));
        client.setProtocolMappers(List.of(audienceMapper(CLIENT_TEST_RESTRICTED)));
        testRealm.clients().create(client).close();
    }

    private static void restrictRequester(RealmResource testRealm) {
        ClientResource requester = client(testRealm, CLIENT_EXCHANGE_RESTRICTED);
        RoleRepresentation role = new RoleRepresentation();
        role.setName(CLIENT_ROLE_RESTRICTED_ACCESS);
        requester.roles().create(role);
        String requesterId = requester.toRepresentation().getId();
        String userId = testRealm.users().search(USER_TEST_UNRESTRICTED, true).get(0).getId();
        testRealm.users().get(userId).roles().clientLevel(requesterId)
            .add(List.of(requester.roles().get(CLIENT_ROLE_RESTRICTED_ACCESS).toRepresentation()));
    }

    private static ProtocolMapperRepresentation audienceMapper(String audience) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("audience " + audience);
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-audience-mapper");
        mapper.setConfig(Map.of(
            "included.client.audience", audience,
            "access.token.claim", "true",
            "introspection.token.claim", "true"));
        return mapper;
    }

    private static ClientResource client(RealmResource testRealm, String clientId) {
        return testRealm.clients().get(testRealm.clients().findByClientId(clientId).get(0).getId());
    }

}
