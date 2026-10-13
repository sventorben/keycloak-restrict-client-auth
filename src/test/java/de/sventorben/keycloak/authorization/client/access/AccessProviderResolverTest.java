package de.sventorben.keycloak.authorization.client.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.UserModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessProviderResolverTest {

    @Mock
    KeycloakSession keycloakSession;

    @Mock
    AccessProvider configuredProvider;

    @Mock
    AccessProvider defaultProvider;

    @Mock
    AccessProvider clientRoleProvider;

    @InjectMocks
    AccessProviderResolver cut;

    @Test
    void returnsConfiguredProvider() {
        given(keycloakSession.getProvider(AccessProvider.class, "policy")).willReturn(configuredProvider);

        assertThat(cut.resolve("policy", "alias")).isSameAs(configuredProvider);
    }

    @Test
    void fallsBackToServerWideDefaultIfConfiguredProviderDoesNotExist() {
        given(keycloakSession.getProvider(AccessProvider.class, "unknown")).willReturn(null);
        given(keycloakSession.getProvider(AccessProvider.class)).willReturn(defaultProvider);

        assertThat(cut.resolve("unknown", "alias")).isSameAs(defaultProvider);
    }

    @Test
    void fallsBackToServerWideDefaultIfNoProviderIsConfigured() {
        given(keycloakSession.getProvider(AccessProvider.class)).willReturn(defaultProvider);

        assertThat(cut.resolve(null, null)).isSameAs(defaultProvider);
    }

    @Test
    void fallsBackToClientRoleProviderIfNoServerWideDefaultExists() {
        given(keycloakSession.getProvider(AccessProvider.class, "unknown")).willReturn(null);
        given(keycloakSession.getProvider(AccessProvider.class)).willReturn(null);
        given(keycloakSession.getProvider(AccessProvider.class, "client-role")).willReturn(clientRoleProvider);

        assertThat(cut.resolve("unknown", "alias")).isSameAs(clientRoleProvider);
    }

    @Nested
    class GivenRealmSettings {

        @Mock
        KeycloakContext keycloakContext;

        @Mock
        RealmModel realm;

        @Mock
        ClientModel client;

        @Mock
        UserModel user;

        @BeforeEach
        void setUp() {
            given(keycloakSession.getContext()).willReturn(keycloakContext);
            given(keycloakContext.getRealm()).willReturn(realm);
        }

        @Test
        void usesAccessProviderOfEnforcementPointIfAccessIsNotDeniedByDefault() {
            givenRealmSettings(Map.of());
            given(keycloakSession.getProvider(AccessProvider.class, "policy")).willReturn(configuredProvider);

            assertThat(cut.resolve("policy", "alias")).isSameAs(configuredProvider);
        }

        @Test
        void deniesAccessByDefaultToClientsNotRestrictedByAccessProviderOfEnforcementPoint() {
            givenRealmSettings(Map.of("defaultDeny", "true"));
            given(keycloakSession.getProvider(AccessProvider.class, "policy")).willReturn(configuredProvider);
            given(configuredProvider.isRestricted(client)).willReturn(false);
            given(client.getRealm()).willReturn(realm);

            AccessProvider accessProvider = cut.resolve("policy", "alias");

            assertThat(accessProvider.isPermitted(client, user)).isFalse();
        }

        @Test
        void deniesAccessToAllClientsIfAccessProviderDoesNotExistAndAccessIsDeniedByDefault() {
            givenRealmSettings(Map.of("defaultDeny", "true"));
            given(keycloakSession.getProvider(AccessProvider.class, "unknown")).willReturn(null);
            given(client.getRealm()).willReturn(realm);

            AccessProvider accessProvider = cut.resolve("unknown", "alias");

            assertThat(accessProvider.isPermitted(client, user)).isFalse();
            verify(keycloakSession, never()).getProvider(AccessProvider.class);
        }

        private void givenRealmSettings(Map<String, String> config) {
            RequiredActionConfigModel configModel = new RequiredActionConfigModel();
            configModel.setConfig(config);
            given(realm.getRequiredActionConfigByAlias("restrict-client-auth-enforce")).willReturn(configModel);
        }
    }

}
