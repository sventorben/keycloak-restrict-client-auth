package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestrictClientAuthRequiredActionTest {

    @Mock
    AccessProviderResolver accessProviderResolver;

    @Mock
    RequiredActionContext context;

    @Mock
    AuthenticationSessionModel authenticationSession;

    @Mock
    ClientModel client;

    @Mock
    UserModel user;

    @InjectMocks
    RestrictClientAuthRequiredAction cut;

    @Test
    void cannotBeInitiatedByApplications() {
        assertThat(cut.initiatedActionSupport()).isEqualTo(InitiatedActionSupport.NOT_SUPPORTED);
    }

    @Test
    void deniesAccessIfFormIsSubmitted(@Mock Response errorPage) {
        givenErrorPage(errorPage);

        cut.processAction(context);

        verify(context).challenge(errorPage);
        verify(context, never()).success();
    }

    @Nested
    class GivenRestrictedClient {

        @Mock
        AccessProvider accessProvider;

        @BeforeEach
        void setUp() {
            given(accessProviderResolver.resolve("client-role", null)).willReturn(accessProvider);
            given(accessProvider.isRestricted(client)).willReturn(true);
        }

        @Test
        void triggersActionIfUserHasNoAccess() {
            givenAccess(false);

            cut.evaluateTriggers(context);

            verify(authenticationSession).addRequiredAction(RestrictClientAuthRequiredActionFactory.PROVIDER_ID);
        }

        @Test
        void doesNotTriggerActionIfUserHasAccess() {
            givenAccess(true);

            cut.evaluateTriggers(context);

            verify(authenticationSession, never()).addRequiredAction(anyString());
        }

        @Test
        void succeedsIfUserHasAccessWhenActionIsExecuted() {
            givenAccess(true);

            cut.requiredActionChallenge(context);

            verify(context).success();
        }

        @Test
        void deniesAccessIfUserHasNoAccessWhenActionIsExecuted(@Mock Response errorPage) {
            givenAccess(false);
            EventBuilder event = givenErrorPage(errorPage);

            cut.requiredActionChallenge(context);

            verify(event).error(Errors.ACCESS_DENIED);
            verify(context).challenge(errorPage);
            verify(context, never()).success();
        }

        private void givenAccess(boolean permitted) {
            givenUserAndClient();
            given(accessProvider.isPermitted(client, user)).willReturn(permitted);
        }
    }

    private EventBuilder givenErrorPage(Response errorPage) {
        EventBuilder event = mock(EventBuilder.class, Answers.RETURNS_SELF);
        LoginFormsProvider form = mock(LoginFormsProvider.class, Answers.RETURNS_SELF);
        givenUserAndClient();
        given(context.getEvent()).willReturn(event);
        given(context.form()).willReturn(form);
        given(form.createErrorPage(Response.Status.FORBIDDEN)).willReturn(errorPage);
        return event;
    }

    private void givenUserAndClient() {
        given(context.getAuthenticationSession()).willReturn(authenticationSession);
        given(authenticationSession.getClient()).willReturn(client);
        given(context.getUser()).willReturn(user);
    }

}
