package de.sventorben.keycloak.authorization.client.requiredactions;

import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;

public class RestrictAccessRequiredAction implements RequiredActionProvider {

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext requiredActionContext) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext requiredActionContext) {
        requiredActionContext.challenge(htmlErrorResponse(requiredActionContext));
    }

    @Override
    public void processAction(RequiredActionContext requiredActionContext) {
    }

    private Response htmlErrorResponse(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        return context.form()
            .setError(Messages.ACCESS_DENIED, authSession.getAuthenticatedUser().getUsername(),
                authSession.getClient().getClientId())
            .createErrorPage(Response.Status.FORBIDDEN);
    }

    @Override
    public void close() {

    }
}
