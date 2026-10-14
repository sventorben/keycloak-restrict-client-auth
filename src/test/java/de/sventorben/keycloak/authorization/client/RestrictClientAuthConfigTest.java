package de.sventorben.keycloak.authorization.client;

import org.junit.jupiter.api.Test;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.services.messages.Messages;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RestrictClientAuthConfigTest {

    @Test
    void doesNotThrowIfConfigModelIsNull() {
        RestrictClientAuthConfig cut = new RestrictClientAuthConfig((AuthenticatorConfigModel) null);
        assertThatCode(cut::getAuthenticatorConfigAlias).doesNotThrowAnyException();
    }

    @Test
    void usesDefaultsIfRequiredActionConfigModelIsNull() {
        RestrictClientAuthConfig cut = new RestrictClientAuthConfig((RequiredActionConfigModel) null);
        assertThat(cut.getAccessProviderId()).isNull();
        assertThat(cut.getErrorMessage()).isEqualTo(Messages.ACCESS_DENIED);
        assertThat(cut.getAuthenticatorConfigAlias()).isNull();
    }

    @Test
    void doesNotDefaultAccessProviderSoThatServerWideDefaultApplies() {
        AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
        configModel.setConfig(Map.of("restrictClientAuthErrorMessage", "test-message"));
        RestrictClientAuthConfig cut = new RestrictClientAuthConfig(configModel);
        assertThat(cut.getAccessProviderId()).isNull();
    }

}
