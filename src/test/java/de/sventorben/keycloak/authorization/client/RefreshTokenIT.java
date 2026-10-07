package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RefreshTokenIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenIT.class);

    private static final String CLIENT_ROLE_RESTRICTED_ACCESS = "restricted-access";

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
        LOGGER.info("Running test with Keycloak image: " + FullImageName.get());
        TOKEN_ENDPOINT = new TokenEndpoint(KEYCLOAK_AUTH_URL);
        try (Keycloak admin = keycloakAdmin()) {
            EnforceAccessPolicy.enable(admin.realm(REALM_TEST));
        }
    }

    @AfterEach
    void restoreRole() {
        try (Keycloak admin = keycloakAdmin()) {
            RealmResource testRealm = admin.realm(REALM_TEST);
            String clientId = testRealm.clients().findByClientId(CLIENT_TEST_RESTRICTED).get(0).getId();
            testRealm.users().get(userId(testRealm)).roles().clientLevel(clientId)
                .add(List.of(restrictedAccessRole(testRealm)));
        }
    }

    @Test
    void refreshForUserWithAccessIsAllowed() throws Exception {
        AccessTokenResponse tokens = TOKEN_ENDPOINT.grantToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_TEST_RESTRICTED, null);

        HttpResponse<String> response = TOKEN_ENDPOINT.refresh(CLIENT_TEST_RESTRICTED, tokens.getRefreshToken());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    @Test
    void refreshForUserWithAccessIsAllowedIfAccessTokenHasNoSubject() throws Exception {
        setBasicClientScope(false);
        try {
            AccessTokenResponse tokens = TOKEN_ENDPOINT.grantToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
                CLIENT_TEST_RESTRICTED, null);
            assertThat(new JWSInput(tokens.getToken()).readJsonContent(AccessToken.class).getSubject()).isNull();

            HttpResponse<String> response = TOKEN_ENDPOINT.refresh(CLIENT_TEST_RESTRICTED, tokens.getRefreshToken());

            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        } finally {
            setBasicClientScope(true);
        }
    }

    @Test
    void refreshForUserWhoLostAccessIsDenied() throws Exception {
        assertRefreshIsDeniedAfterLosingAccess(null);
    }

    @Test
    void offlineTokenRefreshForUserWhoLostAccessIsDenied() throws Exception {
        assertRefreshIsDeniedAfterLosingAccess("offline_access");
    }

    @Test
    void refreshOnUnrestrictedClientIsAllowed() throws Exception {
        AccessTokenResponse tokens = TOKEN_ENDPOINT.grantToken(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED,
            CLIENT_TEST_UNRESTRICTED, null);

        HttpResponse<String> response = TOKEN_ENDPOINT.refresh(CLIENT_TEST_UNRESTRICTED, tokens.getRefreshToken());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private static void assertRefreshIsDeniedAfterLosingAccess(String scope) throws Exception {
        AccessTokenResponse tokens = TOKEN_ENDPOINT.grantToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED,
            CLIENT_TEST_RESTRICTED, scope);
        assertThat(tokens.getRefreshToken()).isNotNull();
        removeRole();

        HttpResponse<String> response = TOKEN_ENDPOINT.refresh(CLIENT_TEST_RESTRICTED, tokens.getRefreshToken());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("invalid_grant", "Access to client is denied.");
    }

    private static void removeRole() {
        try (Keycloak admin = keycloakAdmin()) {
            RealmResource testRealm = admin.realm(REALM_TEST);
            String clientId = testRealm.clients().findByClientId(CLIENT_TEST_RESTRICTED).get(0).getId();
            testRealm.users().get(userId(testRealm)).roles().clientLevel(clientId)
                .remove(List.of(restrictedAccessRole(testRealm)));
        }
    }

    private static void setBasicClientScope(boolean enabled) {
        try (Keycloak admin = keycloakAdmin()) {
            RealmResource testRealm = admin.realm(REALM_TEST);
            String clientId = testRealm.clients().findByClientId(CLIENT_TEST_RESTRICTED).get(0).getId();
            testRealm.clientScopes().findAll().stream()
                .filter(it -> "basic".equals(it.getName()))
                .findFirst()
                .ifPresent(basic -> {
                    if (enabled) {
                        testRealm.clients().get(clientId).addDefaultClientScope(basic.getId());
                    } else {
                        testRealm.clients().get(clientId).removeDefaultClientScope(basic.getId());
                    }
                });
        }
    }

    private static String userId(RealmResource testRealm) {
        return testRealm.users().search(USER_TEST_UNRESTRICTED, true).get(0).getId();
    }

    private static RoleRepresentation restrictedAccessRole(RealmResource testRealm) {
        String clientId = testRealm.clients().findByClientId(CLIENT_TEST_RESTRICTED).get(0).getId();
        return testRealm.clients().get(clientId).roles().get(CLIENT_ROLE_RESTRICTED_ACCESS).toRepresentation();
    }

    private static Keycloak keycloakAdmin() {
        return TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL);
    }

}
