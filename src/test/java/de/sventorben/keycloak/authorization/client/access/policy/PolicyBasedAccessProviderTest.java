package de.sventorben.keycloak.authorization.client.access.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PolicyBasedAccessProviderTest {

    @Mock
    KeycloakSession keycloakSession;

    @Mock
    AuthorizationProvider authorizationProvider;

    @Mock
    StoreFactory storeFactory;

    @Mock
    ResourceServerStore resourceServerStore;

    @Mock
    ResourceStore resourceStore;

    @Mock
    ClientModel client;

    @InjectMocks
    PolicyBasedAccessProvider cut;

    @BeforeEach
    void setUp() {
        given(keycloakSession.getProvider(AuthorizationProvider.class)).willReturn(authorizationProvider);
        given(authorizationProvider.getStoreFactory()).willReturn(storeFactory);
        given(storeFactory.getResourceServerStore()).willReturn(resourceServerStore);
    }

    @Test
    void doesNotChangeClientIfAuthorizationIsNotEnabled(@Mock RealmModel realm) {
        given(resourceServerStore.findByClient(client)).willReturn(null);
        given(client.getRealm()).willReturn(realm);

        cut.enableFor(client);

        verify(storeFactory, never()).getResourceStore();
        verifyClientSettingsUnchanged();
    }

    @Test
    void createsResourceIfAuthorizationIsEnabled(@Mock ResourceServer resourceServer) {
        given(resourceServerStore.findByClient(client)).willReturn(resourceServer);
        given(storeFactory.getResourceStore()).willReturn(resourceStore);
        given(resourceStore.findByName(resourceServer, "Keycloak Client Resource")).willReturn(null);
        given(resourceServer.getClientId()).willReturn("resource-server-id");

        cut.enableFor(client);

        verify(resourceStore).create(resourceServer, "Keycloak Client Resource", "resource-server-id");
        verifyClientSettingsUnchanged();
    }

    private void verifyClientSettingsUnchanged() {
        verify(client, never()).setPublicClient(anyBoolean());
        verify(client, never()).setBearerOnly(anyBoolean());
    }

}
