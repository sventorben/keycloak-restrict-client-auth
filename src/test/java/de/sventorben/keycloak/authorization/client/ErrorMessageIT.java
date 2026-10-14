package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static de.sventorben.keycloak.authorization.client.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The error message is looked up as a message key, so it can be defined via realm overrides without a custom theme. The
 * username and the client ID are passed as parameters {0} and {1}.
 */
@Testcontainers
class ErrorMessageIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorMessageIT.class);

    private static final String REDIRECT_URI = "http://localhost/callback";
    private static final String MESSAGE_KEY = "restricted-access.denied";

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
        try (Keycloak admin = TestConstants.keycloakAdmin(KEYCLOAK_AUTH_URL)) {
            RealmResource realm = admin.realm(REALM_TEST);
            realm.localization().saveRealmLocalizationText("en", MESSAGE_KEY, "User {0} may not access {1}.");
            RequiredActionConfigRepresentation config = new RequiredActionConfigRepresentation();
            config.setConfig(Map.of("restrictClientAuthErrorMessage", MESSAGE_KEY));
            realm.flows().updateRequiredActionConfig(RequiredActions.RESTRICT_CLIENT_AUTH, config);
        }
    }

    @Test
    void showsErrorMessageDefinedViaRealmOverrides() throws Exception {
        BrowserLogin browser = new BrowserLogin(KEYCLOAK_AUTH_URL);

        HttpResponse<String> response = browser.login(browser.authorize(CLIENT_TEST_RESTRICTED, REDIRECT_URI),
            USER_TEST_RESTRICTED, PASS_TEST_RESTRICTED);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
        assertThat(response.body())
            .contains("User " + USER_TEST_RESTRICTED + " may not access " + CLIENT_TEST_RESTRICTED + ".");
    }

}
