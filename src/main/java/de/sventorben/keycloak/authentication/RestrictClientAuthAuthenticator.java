package de.sventorben.keycloak.authentication;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.*;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.utils.MediaTypeMatcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public final class RestrictClientAuthAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(RestrictClientAuthAuthenticator.class);

    private final String clientRoleName;

    RestrictClientAuthAuthenticator(final String clientRoleName) {
        this.clientRoleName = clientRoleName;
    }

    @Override
    public void authenticate(final AuthenticationFlowContext context) {
        final ClientModel client = context.getSession().getContext().getClient();

        if (!isAuthRestricted(client)) {
            context.success();
            return;
        }

        final UserModel user = context.getUser();
        if (userHasClientRole(client, user)) {
            context.success();
        } else {
            LOG.warnf("Authentication for user '%s' failed. User does not have client role '%s' on client '%s'.",
                    user.getUsername(), clientRoleName, client.getId());
            context.getEvent().client(client).user(context.getUser()).realm(context.getRealm()).error(Errors.ACCESS_DENIED);
            context.failure(AuthenticationFlowError.ACCESS_DENIED,  errorResponse(context));
        }
    }

    private Response errorResponse(AuthenticationFlowContext context) {
        Response response;
        if (MediaTypeMatcher.isHtmlRequest(context.getHttpRequest().getHttpHeaders())) {
            response = htmlErrorResponse(context);
        } else {
            response = oAuth2ErrorResponse();
        }
        return response;
    }

    private Response htmlErrorResponse(AuthenticationFlowContext context) {
        return context.form()
                .setError(Messages.ACCESS_DENIED)
                .createErrorPage(Response.Status.FORBIDDEN);
    }

    private static Response oAuth2ErrorResponse() {
        return Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                .entity(new OAuth2ErrorRepresentation(Messages.ACCESS_DENIED, "Access to client is denied."))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    private boolean isAuthRestricted(ClientModel client) {
        return client.getRole(clientRoleName) != null;
    }

    private boolean userHasClientRole(ClientModel client, UserModel user) {
        final RoleModel role = client.getRole(clientRoleName);
        if (role == null) return false;
        return user != null && user.hasRole(role);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        LOG.warn("Action called!");
        context.failure(AuthenticationFlowError.ACCESS_DENIED);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}
