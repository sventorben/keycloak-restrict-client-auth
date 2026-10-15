package de.sventorben.keycloak.authorization.client.access.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EnforcingResourceServerTest {

    @Mock
    ResourceServer resourceServer;

    @ParameterizedTest
    @EnumSource(PolicyEnforcementMode.class)
    void alwaysEnforcesPolicies(PolicyEnforcementMode policyEnforcementMode) {
        lenient().when(resourceServer.getPolicyEnforcementMode()).thenReturn(policyEnforcementMode);

        assertThat(new EnforcingResourceServer(resourceServer).getPolicyEnforcementMode())
            .isEqualTo(PolicyEnforcementMode.ENFORCING);
    }

    @Test
    void delegatesEverythingElse() {
        given(resourceServer.getId()).willReturn("id");
        given(resourceServer.getClientId()).willReturn("client-id");
        given(resourceServer.getDecisionStrategy()).willReturn(DecisionStrategy.AFFIRMATIVE);
        given(resourceServer.isAllowRemoteResourceManagement()).willReturn(true);

        EnforcingResourceServer cut = new EnforcingResourceServer(resourceServer);

        assertThat(cut.getId()).isEqualTo("id");
        assertThat(cut.getClientId()).isEqualTo("client-id");
        assertThat(cut.getDecisionStrategy()).isEqualTo(DecisionStrategy.AFFIRMATIVE);
        assertThat(cut.isAllowRemoteResourceManagement()).isTrue();
    }
}
