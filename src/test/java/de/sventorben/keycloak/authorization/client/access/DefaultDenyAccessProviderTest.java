package de.sventorben.keycloak.authorization.client.access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DefaultDenyAccessProviderTest {

    private static final String DEFAULT_ACCESS_ROLE = "default-access";

    @Mock
    AccessProvider accessProvider;

    @Mock
    ClientModel client;

    @Mock
    RealmModel realm;

    @Mock
    UserModel user;

    @Test
    void restrictsAllClients() {
        given(client.getRealm()).willReturn(realm);
        given(realm.getName()).willReturn("test-realm");

        assertThat(new DefaultDenyAccessProvider(accessProvider, null).isRestricted(client)).isTrue();
    }

    @Test
    void decidesAccessToClientRestrictedByAccessProvider() {
        given(accessProvider.isRestricted(client)).willReturn(true);
        given(accessProvider.isPermitted(client, user)).willReturn(true);

        assertThat(new DefaultDenyAccessProvider(accessProvider, null).isPermitted(client, user)).isTrue();
    }

    @Test
    void deniesAccessToClientNotRestrictedByAccessProviderWithoutDefaultAccessRole() {
        given(accessProvider.isRestricted(client)).willReturn(false);
        given(client.getRealm()).willReturn(realm);

        assertThat(new DefaultDenyAccessProvider(accessProvider, null).isPermitted(client, user)).isFalse();
    }

    @Test
    void permitsAccessToClientNotRestrictedByAccessProviderForUserWithDefaultAccessRole(@Mock RoleModel role) {
        givenDefaultAccessRole(role);
        given(user.hasRole(role)).willReturn(true);

        assertThat(withDefaultAccessRole().isPermitted(client, user)).isTrue();
    }

    @Test
    void deniesAccessToClientNotRestrictedByAccessProviderForUserWithoutDefaultAccessRole(@Mock RoleModel role) {
        givenDefaultAccessRole(role);
        given(user.hasRole(role)).willReturn(false);

        assertThat(withDefaultAccessRole().isPermitted(client, user)).isFalse();
    }

    @Test
    void deniesAccessIfDefaultAccessRoleDoesNotExist() {
        given(accessProvider.isRestricted(client)).willReturn(false);
        given(client.getRealm()).willReturn(realm);
        given(realm.getRole(DEFAULT_ACCESS_ROLE)).willReturn(null);

        assertThat(withDefaultAccessRole().isPermitted(client, user)).isFalse();
    }

    @Test
    void deniesAccessWithoutAccessProvider() {
        given(client.getRealm()).willReturn(realm);

        assertThat(new DefaultDenyAccessProvider(null, null).isPermitted(client, user)).isFalse();
    }

    private void givenDefaultAccessRole(RoleModel role) {
        given(accessProvider.isRestricted(client)).willReturn(false);
        given(client.getRealm()).willReturn(realm);
        given(realm.getRole(DEFAULT_ACCESS_ROLE)).willReturn(role);
    }

    private DefaultDenyAccessProvider withDefaultAccessRole() {
        return new DefaultDenyAccessProvider(accessProvider, DEFAULT_ACCESS_ROLE);
    }

}
