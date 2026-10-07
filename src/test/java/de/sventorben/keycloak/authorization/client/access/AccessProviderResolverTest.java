package de.sventorben.keycloak.authorization.client.access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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

}
