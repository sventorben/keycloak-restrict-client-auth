package de.sventorben.keycloak.authorization.client;

import org.keycloak.admin.client.Keycloak;

class TestConstants {

    static final int KEYCLOAK_HTTP_PORT = 8080;

    static final String KEYCLOAK_ADMIN_PASS = "admin";
    static final String KEYCLOAK_ADMIN_USER = "admin";

    static final String REALM_TEST = "test-realm";
    static final String CLIENT_TEST_RESTRICTED = "test-client-restricted";
    static final String CLIENT_TEST_RESTRICTED_BY_POLICY = "test-client-restricted-by-policy";
    static final String CLIENT_SECRET_TEST_RESTRICTED_BY_POLICY = "42437f49-2b56-498e-a67c-13d4ee2d8cad";
    static final String CLIENT_TEST_UNRESTRICTED = "test-client-unrestricted";
    static final String USER_TEST_RESTRICTED = "test-restricted";
    static final String PASS_TEST_RESTRICTED = "test";
    static final String USER_TEST_UNRESTRICTED = "test-unrestricted";
    static final String PASS_TEST_UNRESTRICTED = "test";

    static Keycloak keycloakAdmin(String serverUrl) {
        return keycloak(serverUrl, "master", KEYCLOAK_ADMIN_USER, KEYCLOAK_ADMIN_PASS, "admin-cli", null);
    }

    static Keycloak keycloakTest(String serverUrl, String username, String password, String client) {
        return keycloakTest(serverUrl, username, password, client, null);
    }

    static Keycloak keycloakTest(String serverUrl, String username, String password, String client, String clientSecret) {
        return keycloak(serverUrl, REALM_TEST, username, password, client, clientSecret);
    }

    static Keycloak keycloak(String serverUrl, String realm, String username, String password, String client, String clientSecret) {
        return Keycloak.getInstance(serverUrl, realm, username, password, client, clientSecret);
    }
}
