package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.BrowserLogin.UPDATE_PASSWORD_FORM;
import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test realm uses Keycloak's built-in browser flow without the authenticator. Hence, only the required action
 * restricts access.
 */
@Testcontainers
class RequiredActionIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequiredActionIT.class);

    private static final String REDIRECT_URI = "http://localhost/callback";

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
        try (Keycloak admin = keycloakAdmin()) {
            allowRedirectsOfUnrestrictedClient(admin.realm(REALM_TEST));
        }
    }

    @AfterEach
    void removeRequiredActionsOfRestrictedUser() {
        setRequiredActionsOfRestrictedUser(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {REALM_TEST, "master"})
    void isRegisteredAndEnabledInAllRealmsByDefault(String realm) {
        try (Keycloak admin = keycloakAdmin()) {
            RequiredActionProviderRepresentation requiredAction =
                admin.realm(realm).flows().getRequiredAction(RequiredActions.RESTRICT_CLIENT_AUTH);

            assertThat(requiredAction.isEnabled()).isTrue();
        }
    }

    @Test
    void loginForUserWithAccessIsAllowed() throws Exception {
        BrowserLogin browser = new BrowserLogin(KEYCLOAK_AUTH_URL);

        HttpResponse<String> response = browser.login(browser.authorize(CLIENT_TEST_RESTRICTED, REDIRECT_URI),
            USER_TEST_UNRESTRICTED, PASS_TEST_UNRESTRICTED);

        assertThat(BrowserLogin.code(response)).isNotEmpty();
    }

    @Test
    void loginForUserWithoutAccessIsDenied() throws Exception {
        BrowserLogin browser = new BrowserLogin(KEYCLOAK_AUTH_URL);

        HttpResponse<String> response = browser.login(browser.authorize(CLIENT_TEST_RESTRICTED, REDIRECT_URI),
            USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED);

        assertDenied(response);
    }

    @Test
    void singleSignOnForUserWithoutAccessIsDenied() throws Exception {
        BrowserLogin browser = new BrowserLogin(KEYCLOAK_AUTH_URL);
        browser.authorizationCode(CLIENT_TEST_UNRESTRICTED, REDIRECT_URI,
            USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED);

        HttpResponse<String> response = browser.authorize(CLIENT_TEST_RESTRICTED, REDIRECT_URI);

        assertDenied(response);
    }

    @Test
    void otherRequiredActionsAreCompletedBeforeAccessIsDenied() throws Exception {
        setRequiredActionsOfRestrictedUser(List.of("UPDATE_PASSWORD"));
        BrowserLogin browser = new BrowserLogin(KEYCLOAK_AUTH_URL);

        HttpResponse<String> updatePasswordPage = browser.login(
            browser.authorize(CLIENT_TEST_RESTRICTED, REDIRECT_URI), USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED);
        HttpResponse<String> response = browser.submitForm(updatePasswordPage, UPDATE_PASSWORD_FORM,
            Map.of("password-new", PASS_TEST_RESTRICTED, "password-confirm", PASS_TEST_RESTRICTED));

        assertDenied(response);
    }

    @Test
    void showsErrorMessageAfterPasswordUpdateIfConfigured() throws Exception {
        setRequiredActionsOfRestrictedUser(List.of("UPDATE_PASSWORD"));
        setRequiredActionConfig(Map.of("passwordUpdatedErrorMessage", "Password updated, but access is denied."));
        try {
            BrowserLogin browser = new BrowserLogin(KEYCLOAK_AUTH_URL);

            HttpResponse<String> updatePasswordPage = browser.login(
                browser.authorize(CLIENT_TEST_RESTRICTED, REDIRECT_URI), USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED);
            HttpResponse<String> response = browser.submitForm(updatePasswordPage, UPDATE_PASSWORD_FORM,
                Map.of("password-new", PASS_TEST_RESTRICTED, "password-confirm", PASS_TEST_RESTRICTED));

            assertDenied(response);
            assertThat(response.body()).contains("Password updated, but access is denied.");
        } finally {
            setRequiredActionConfig(Map.of());
        }
    }

    @Test
    void loginToClientRestrictedByPolicyForUserWithoutPermissionIsDenied() throws Exception {
        BrowserLogin browser = new BrowserLogin(KEYCLOAK_AUTH_URL);

        HttpResponse<String> response = browser.login(browser.authorize(CLIENT_TEST_RESTRICTED_BY_POLICY, REDIRECT_URI),
            USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED);

        assertDenied(response);
    }

    private static void allowRedirectsOfUnrestrictedClient(RealmResource testRealm) {
        ClientRepresentation client = testRealm.clients().findByClientId(CLIENT_TEST_UNRESTRICTED).get(0);
        client.setRedirectUris(List.of(REDIRECT_URI));
        testRealm.clients().get(client.getId()).update(client);
    }

    private static void assertDenied(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
    }

    private static void setRequiredActionsOfRestrictedUser(List<String> requiredActions) {
        try (Keycloak admin = keycloakAdmin()) {
            String userId = admin.realm(REALM_TEST).users().search(USER_TEST_RESTRICTED, true).get(0).getId();
            UserResource user = admin.realm(REALM_TEST).users().get(userId);
            UserRepresentation representation = user.toRepresentation();
            representation.setRequiredActions(requiredActions);
            user.update(representation);
        }
    }

    private static void setRequiredActionConfig(Map<String, String> settings) {
        try (Keycloak admin = keycloakAdmin()) {
            RequiredActionConfigRepresentation config = new RequiredActionConfigRepresentation();
            config.setConfig(settings);
            admin.realm(REALM_TEST).flows().updateRequiredActionConfig(RequiredActions.RESTRICT_CLIENT_AUTH, config);
        }
    }

    private static Keycloak keycloakAdmin() {
        return TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL);
    }

}
