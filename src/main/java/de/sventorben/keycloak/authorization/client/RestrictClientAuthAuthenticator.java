package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.MediaTypeMatcher;

final class RestrictClientAuthAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(RestrictClientAuthAuthenticator.class);

    private static final String TOKEN_ENDPOINT_PATH = "/protocol/openid-connect/token";

    private final AccessProviderResolver accessProviderResolver;

    RestrictClientAuthAuthenticator(AccessProviderResolver accessProviderResolver) {
        this.accessProviderResolver = accessProviderResolver;
    }

    @Override
    public void authenticate(final AuthenticationFlowContext context) {
        final ClientModel client = context.getSession().getContext().getClient();
        final RestrictClientAuthConfig config = new RestrictClientAuthConfig(context.getAuthenticatorConfig());

        final AccessProvider access = accessProviderResolver.resolve(
            config.getAccessProviderId(), config.getAuthenticatorConfigAlias());

        if (!access.isRestricted(client)) {
            context.success();
            return;
        }

        final UserModel user = context.getUser();
        if (access.isPermitted(client, user)) {
            context.success();
        } else {
            context.getEvent()
                .realm(context.getRealm())
                .client(client)
                .user(context.getUser())
                .error(Errors.ACCESS_DENIED);
            context.failure(AuthenticationFlowError.ACCESS_DENIED, errorResponse(context, config));
        }
    }

    private Response errorResponse(AuthenticationFlowContext context, RestrictClientAuthConfig config) {
        Response response;
        if (MediaTypeMatcher.isHtmlRequest(context.getHttpRequest().getHttpHeaders())) {
            response = htmlErrorResponse(context, config);
        } else {
            response = oAuth2ErrorResponse(context);
        }
        return response;
    }

    private Response htmlErrorResponse(AuthenticationFlowContext context, RestrictClientAuthConfig config) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        return context.form()
            .setError(config.getErrorMessage(), authSession.getAuthenticatedUser().getUsername(),
                authSession.getClient().getClientId())
            .createErrorPage(Response.Status.FORBIDDEN);
    }

    /**
     * Errors according to RFC 6749: 'invalid_grant' from the token endpoint, i.e. for the resource owner password
     * credentials grant, and 'access_denied' otherwise.
     */
    private static Response oAuth2ErrorResponse(AuthenticationFlowContext context) {
        if (context.getUriInfo().getPath().endsWith(TOKEN_ENDPOINT_PATH)) {
            return oAuth2ErrorResponse(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_GRANT);
        }
        return oAuth2ErrorResponse(Response.Status.FORBIDDEN, OAuthErrorException.ACCESS_DENIED);
    }

    private static Response oAuth2ErrorResponse(Response.Status status, String error) {
        return Response.status(status.getStatusCode())
            .entity(new OAuth2ErrorRepresentation(error, "Access to client is denied."))
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build();
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
