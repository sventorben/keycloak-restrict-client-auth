package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RoleModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.userprofile.ValidationException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RestrictClientAuthRequiredActionFactoryTest {

    @Mock
    KeycloakSession session;

    @Mock
    KeycloakSessionFactory sessionFactory;

    @Mock
    RealmModel realm;

    RestrictClientAuthRequiredActionFactory cut = new RestrictClientAuthRequiredActionFactory();

    @Test
    void acceptsEmptyConfig() {
        assertThatCode(() -> cut.validateConfig(session, realm, config(Map.of()))).doesNotThrowAnyException();
    }

    @Test
    void acceptsExistingAccessProviderAndRole(@Mock ProviderFactory<AccessProvider> accessProviderFactory,
                                              @Mock RoleModel role) {
        givenAccessProvider("policy", accessProviderFactory);
        given(realm.getRole("default-access")).willReturn(role);

        assertThatCode(() -> cut.validateConfig(session, realm,
            config(Map.of("accessProviderId", "policy", "defaultAccessRole", "default-access"))))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownAccessProvider() {
        givenAccessProvider("unknown", null);

        assertThatThrownBy(() -> cut.validateConfig(session, realm, config(Map.of("accessProviderId", "unknown"))))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsUnknownDefaultAccessRole() {
        given(realm.getRole("unknown")).willReturn(null);

        assertThatThrownBy(() -> cut.validateConfig(session, realm, config(Map.of("defaultAccessRole", "unknown"))))
            .isInstanceOf(ValidationException.class);
    }

    private void givenAccessProvider(String accessProviderId, ProviderFactory<AccessProvider> factory) {
        given(session.getKeycloakSessionFactory()).willReturn(sessionFactory);
        given(sessionFactory.getProviderFactory(AccessProvider.class, accessProviderId)).willReturn(factory);
    }

    private static RequiredActionConfigModel config(Map<String, String> config) {
        RequiredActionConfigModel configModel = new RequiredActionConfigModel();
        configModel.setConfig(config);
        return configModel;
    }

}
