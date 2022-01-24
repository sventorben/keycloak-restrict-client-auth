package de.sventorben.keycloak.authorization.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class RestrictClientAuthConfigTest {

    @Test
    void doesNotThrowIfConfigModelIsNull() {
        RestrictClientAuthConfig cut = new RestrictClientAuthConfig(null);
        assertThatCode(cut::getAuthenticatorConfigAlias).doesNotThrowAnyException();
    }

}
