package de.sventorben.keycloak.authorization.client.access.auto;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AutoAccessProviderTest {

    @Mock
    KeycloakSession keycloakSession;

    @Mock
    AccessProvider clientRoleProvider;

    @Mock
    AccessProvider policyProvider;

    @Mock
    ClientModel client;

    @Mock
    UserModel user;

    AutoAccessProvider cut;

    @BeforeEach
    void setUp() {
        cut = new AutoAccessProvider(keycloakSession);
        // a fixed order, so that each test knows which access providers are asked
        given(keycloakSession.getAllProviders(AccessProvider.class))
            .willReturn(new LinkedHashSet<>(List.of(clientRoleProvider, policyProvider, cut)));
    }

    @Test
    void isNotRestrictedIfNoAccessProviderRestrictsClient() {
        givenRestrictedBy(false, false);

        assertThat(cut.isRestricted(client)).isFalse();
    }

    @Test
    void isRestrictedIfAnyAccessProviderRestrictsClient() {
        givenRestrictedBy(false, true);

        assertThat(cut.isRestricted(client)).isTrue();
    }

    @Test
    void permitsAccessIfAccessProviderRestrictingClientPermitsAccess() {
        givenRestrictedBy(true, false);
        given(clientRoleProvider.isPermitted(client, user)).willReturn(true);

        assertThat(cut.isPermitted(client, user)).isTrue();
    }

    @Test
    void deniesAccessIfAnyAccessProviderRestrictingClientDeniesAccess() {
        givenRestrictedBy(true, true);
        given(clientRoleProvider.isPermitted(client, user)).willReturn(true);
        given(policyProvider.isPermitted(client, user)).willReturn(false);

        assertThat(cut.isPermitted(client, user)).isFalse();
    }

    @Test
    void deniesAccessIfNoAccessProviderRestrictsClient() {
        givenRestrictedBy(false, false);

        assertThat(cut.isPermitted(client, user)).isFalse();
    }

    private void givenRestrictedBy(boolean clientRole, boolean policy) {
        given(clientRoleProvider.isRestricted(client)).willReturn(clientRole);
        given(policyProvider.isRestricted(client)).willReturn(policy);
    }

}
