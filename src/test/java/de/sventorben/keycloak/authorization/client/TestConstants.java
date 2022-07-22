package de.sventorben.keycloak.authorization.client;

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

}
