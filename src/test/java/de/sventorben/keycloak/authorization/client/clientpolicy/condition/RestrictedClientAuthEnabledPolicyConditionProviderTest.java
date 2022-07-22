package de.sventorben.keycloak.authorization.client.clientpolicy.condition;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.services.clientpolicy.ClientPolicyVote.ABSTAIN;
import static org.keycloak.services.clientpolicy.ClientPolicyVote.NO;
import static org.keycloak.services.clientpolicy.ClientPolicyVote.YES;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RestrictedClientAuthEnabledPolicyConditionProviderTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    KeycloakSession session;

    @Mock
    ClientModel clientModel;

    @Mock
    AccessProvider accessProvider;

    @InjectMocks
    RestrictedClientAuthEnabledPolicyConditionProvider cut;

    @ParameterizedTest
    @EnumSource(value = ClientPolicyEvent.class, mode = EnumSource.Mode.EXCLUDE, names = {"REGISTER", "REGISTERED", "UPDATE", "UPDATED"})
    void abstain(ClientPolicyEvent event) {
        ClientPolicyVote clientPolicyVote = cut.applyPolicy(new ClientPolicyContext() {
            @Override
            public ClientPolicyEvent getEvent() {
                return event;
            }
        });
        assertThat(clientPolicyVote).isEqualTo(ABSTAIN);
    }

    @Nested
    class GivenAccessProvider {

        @BeforeEach
        void setUp() {
            given(session.getContext().getClient()).willReturn(clientModel);
            given(session.getAllProviders(AccessProvider.class)).willReturn(Set.of(accessProvider));
        }

        @ParameterizedTest
        @EnumSource(value = ClientPolicyEvent.class, mode = EnumSource.Mode.INCLUDE, names = {"REGISTER", "REGISTERED", "UPDATE", "UPDATED"})
        void isRestricted(ClientPolicyEvent event) {
            given(accessProvider.isRestricted(clientModel)).willReturn(true);

            ClientPolicyVote clientPolicyVote = cut.applyPolicy(new ClientPolicyContext() {
                @Override
                public ClientPolicyEvent getEvent() {
                    return event;
                }
            });

            assertThat(clientPolicyVote).isEqualTo(YES);
        }


        @ParameterizedTest
        @EnumSource(value = ClientPolicyEvent.class, mode = EnumSource.Mode.INCLUDE, names = {"REGISTER", "REGISTERED", "UPDATE", "UPDATED"})
        void isUnrestricted(ClientPolicyEvent event) {
            given(accessProvider.isRestricted(clientModel)).willReturn(false);

            ClientPolicyVote clientPolicyVote = cut.applyPolicy(new ClientPolicyContext() {
                @Override
                public ClientPolicyEvent getEvent() {
                    return event;
                }
            });

            assertThat(clientPolicyVote).isEqualTo(NO);
        }
    }

}
