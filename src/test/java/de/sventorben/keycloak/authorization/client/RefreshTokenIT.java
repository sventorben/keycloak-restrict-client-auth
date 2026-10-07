package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.json.RawJsonValue;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RefreshTokenIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenIT.class);

    private static final String CLIENT_ROLE_RESTRICTED_ACCESS = "restricted-access";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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
        enableEnforceAccessPolicy();
    }

    @AfterEach
    void restoreRole() {
        try (Keycloak admin = keycloakAdmin()) {
            RealmResource testRealm = admin.realm(REALM_TEST);
            String clientId = testRealm.clients().findByClientId(CLIENT_TEST_RESTRICTED).get(0).getId();
            testRealm.users().get(userId(testRealm)).roles().clientLevel(clientId).add(List.of(restrictedAccessRole(testRealm)));
        }
    }

    @Test
    void refreshForUserWithAccessIsAllowed() throws Exception {
        AccessTokenResponse tokens = grantToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED, null);

        HttpResponse<String> response = refresh(CLIENT_TEST_RESTRICTED, tokens.getRefreshToken());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    @Test
    void refreshForUserWithAccessIsAllowedIfAccessTokenHasNoSubject() throws Exception {
        setBasicClientScope(false);
        try {
            AccessTokenResponse tokens = grantToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED, null);
            assertThat(new JWSInput(tokens.getToken()).readJsonContent(AccessToken.class).getSubject()).isNull();

            HttpResponse<String> response = refresh(CLIENT_TEST_RESTRICTED, tokens.getRefreshToken());

            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        } finally {
            setBasicClientScope(true);
        }
    }

    @Test
    void refreshForUserWhoLostAccessIsDenied() throws Exception {
        AccessTokenResponse tokens = grantToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED, null);
        removeRole();

        HttpResponse<String> response = refresh(CLIENT_TEST_RESTRICTED, tokens.getRefreshToken());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("invalid_grant", "Access to client is denied.");
    }

    @Test
    void offlineTokenRefreshForUserWhoLostAccessIsDenied() throws Exception {
        AccessTokenResponse tokens = grantToken(USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED, CLIENT_TEST_RESTRICTED, "offline_access");
        assertThat(tokens.getRefreshToken()).isNotNull();
        removeRole();

        HttpResponse<String> response = refresh(CLIENT_TEST_RESTRICTED, tokens.getRefreshToken());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("invalid_grant", "Access to client is denied.");
    }

    @Test
    void refreshOnUnrestrictedClientIsAllowed() throws Exception {
        AccessTokenResponse tokens = grantToken(USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED, CLIENT_TEST_UNRESTRICTED, null);

        HttpResponse<String> response = refresh(CLIENT_TEST_UNRESTRICTED, tokens.getRefreshToken());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private static void enableEnforceAccessPolicy() {
        try (Keycloak admin = keycloakAdmin()) {
            RealmResource testRealm = admin.realm(REALM_TEST);

            ClientPolicyExecutorRepresentation executor = new ClientPolicyExecutorRepresentation();
            executor.setExecutorProviderId("restrict-client-auth-enforce");
            executor.setConfiguration(RawJsonValue.of(Map.of("accessProviderId", "client-role")));
            ClientProfileRepresentation profile = new ClientProfileRepresentation();
            profile.setName("Enforce restricted access");
            profile.setExecutors(List.of(executor));
            ClientProfilesRepresentation profiles = testRealm.clientPoliciesProfilesResource().getProfiles(false);
            profiles.getProfiles().add(profile);
            testRealm.clientPoliciesProfilesResource().updateProfiles(profiles);

            ClientPolicyConditionRepresentation condition = new ClientPolicyConditionRepresentation();
            condition.setConditionProviderId("any-client");
            condition.setConfiguration(RawJsonValue.of(Map.of()));
            ClientPolicyRepresentation policy = new ClientPolicyRepresentation();
            policy.setName("enforce-restricted-access-all-clients");
            policy.setEnabled(true);
            policy.setConditions(List.of(condition));
            policy.setProfiles(List.of(profile.getName()));
            ClientPoliciesRepresentation policies = testRealm.clientPoliciesPoliciesResource().getPolicies();
            policies.getPolicies().add(policy);
            testRealm.clientPoliciesPoliciesResource().updatePolicies(policies);
        }
    }

    private static void removeRole() {
        try (Keycloak admin = keycloakAdmin()) {
            RealmResource testRealm = admin.realm(REALM_TEST);
            String clientId = testRealm.clients().findByClientId(CLIENT_TEST_RESTRICTED).get(0).getId();
            testRealm.users().get(userId(testRealm)).roles().clientLevel(clientId).remove(List.of(restrictedAccessRole(testRealm)));
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

    private static AccessTokenResponse grantToken(String username, String password, String client, String scope)
        throws IOException, InterruptedException {
        String form = "grant_type=password"
            + "&client_id=" + URLEncoder.encode(client, StandardCharsets.UTF_8)
            + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
            + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        if (scope != null) {
            form += "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
        }
        HttpResponse<String> response = postToTokenEndpoint(form);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JsonSerialization.readValue(response.body(), AccessTokenResponse.class);
    }

    private static HttpResponse<String> refresh(String client, String refreshToken) throws IOException, InterruptedException {
        String form = "grant_type=refresh_token"
            + "&client_id=" + URLEncoder.encode(client, StandardCharsets.UTF_8)
            + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        return postToTokenEndpoint(form);
    }

    private static HttpResponse<String> postToTokenEndpoint(String form) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(KEYCLOAK_AUTH_URL + "/realms/" + REALM_TEST + "/protocol/openid-connect/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Keycloak keycloakAdmin() {
        return TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL);
    }

}
