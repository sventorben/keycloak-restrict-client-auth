package de.sventorben.keycloak.authorization.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequiredActionRegistrationTest {

    @Mock
    RealmModel realm;

    @Test
    void registersAndEnablesRequiredActionAfterAllOtherRequiredActions() {
        given(realm.getRequiredActionProviderByAlias("restrict-client-auth-enforce")).willReturn(null);
        given(realm.getRequiredActionProvidersStream())
            .willReturn(Stream.of(requiredActionWithPriority(10), requiredActionWithPriority(90)));

        RequiredActionRegistration.registerIfAbsent(realm);

        ArgumentCaptor<RequiredActionProviderModel> requiredAction =
            ArgumentCaptor.forClass(RequiredActionProviderModel.class);
        verify(realm).addRequiredActionProvider(requiredAction.capture());
        assertThat(requiredAction.getValue().getAlias()).isEqualTo("restrict-client-auth-enforce");
        assertThat(requiredAction.getValue().getProviderId()).isEqualTo("restrict-client-auth-enforce");
        assertThat(requiredAction.getValue().isEnabled()).isTrue();
        assertThat(requiredAction.getValue().isDefaultAction()).isFalse();
        assertThat(requiredAction.getValue().getPriority()).isEqualTo(91);
    }

    @Test
    void leavesRegisteredRequiredActionUntouched() {
        RequiredActionProviderModel disabled = requiredActionWithPriority(10);
        disabled.setEnabled(false);
        given(realm.getRequiredActionProviderByAlias("restrict-client-auth-enforce")).willReturn(disabled);

        RequiredActionRegistration.registerIfAbsent(realm);

        verify(realm, never()).addRequiredActionProvider(any());
        assertThat(disabled.isEnabled()).isFalse();
    }

    private static RequiredActionProviderModel requiredActionWithPriority(int priority) {
        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setPriority(priority);
        return requiredAction;
    }

}
