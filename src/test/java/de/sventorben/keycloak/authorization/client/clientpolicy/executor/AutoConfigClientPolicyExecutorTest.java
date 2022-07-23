package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoConfigClientPolicyExecutorTest {

    @Mock
    KeycloakSession keycloakSession;

    @Mock
    AccessProvider accessProvider;

    @Mock
    ClientModel targetClient;

    @InjectMocks
    AutoConfigClientPolicyExecutor cut;

    @BeforeEach
    void setUp() {
        cut.setupConfiguration(null);
    }

    @ParameterizedTest
    @EnumSource(value = ClientPolicyEvent.class, mode = EnumSource.Mode.INCLUDE, names = {"REGISTER", "REGISTERED", "UPDATE", "UPDATED"})
    void enable(ClientPolicyEvent event) {
        given(keycloakSession.getProvider(AccessProvider.class, "client-role")).willReturn(accessProvider);
        cut.executeOnEvent(new ClientCRUDContext() {
            @Override
            public ClientPolicyEvent getEvent() {
                return event;
            }

            @Override
            public ClientModel getTargetClient() {
                return targetClient;
            }
        });
        verify(accessProvider).enableFor(targetClient);
    }

    @ParameterizedTest
    @EnumSource(value = ClientPolicyEvent.class, mode = EnumSource.Mode.EXCLUDE, names = {"REGISTER", "REGISTERED", "UPDATE", "UPDATED"})
    void doNothing(ClientPolicyEvent event) {
        cut.executeOnEvent(new ClientCRUDContext() {
            @Override
            public ClientPolicyEvent getEvent() {
                return event;
            }

            @Override
            public ClientModel getTargetClient() {
                return targetClient;
            }
        });
        verify(accessProvider, never()).enableFor(targetClient);
    }

}
