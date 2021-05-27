package de.sventorben.keycloak.authentication;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

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
            final Response response = errorResponse("access_denied", "Access to client is denied.");
            context.failure(AuthenticationFlowError.CLIENT_DISABLED, response);
        }
    }

    private boolean isAuthRestricted(ClientModel client) {
        return client.getRole(clientRoleName) != null;
    }

    private boolean userHasClientRole(ClientModel client, UserModel user) {
        final RoleModel role = client.getRole(clientRoleName);
        if (role == null) return false;
        return user != null && user.hasRole(role);
    }

    private static Response errorResponse(String error, String errorDescription) {
        return Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                .entity(new OAuth2ErrorRepresentation(error, errorDescription))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        LOG.warn("Action called!");
        context.failure(AuthenticationFlowError.CLIENT_DISABLED);
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
