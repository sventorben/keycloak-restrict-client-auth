package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.MediaTypeMatcher;

final class RestrictClientAuthAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(RestrictClientAuthAuthenticator.class);

    RestrictClientAuthAuthenticator() {
    }

    @Override
    public void authenticate(final AuthenticationFlowContext context) {
        final ClientModel client = context.getSession().getContext().getClient();
        final RestrictClientAuthConfig config = new RestrictClientAuthConfig(context.getAuthenticatorConfig());

        final AccessProvider access = getAccessProvider(context, config);

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

    private AccessProvider getAccessProvider(AuthenticationFlowContext context, RestrictClientAuthConfig config) {
        final String accessProviderId = config.getAccessProviderId();

        if (accessProviderId != null) {
            AccessProvider accessProvider = context.getSession().getProvider(AccessProvider.class, accessProviderId);
            if (accessProvider == null) {
                LOG.warnf(
                    "Configured access provider '%s' in authenticator config '%s' does not exist.",
                    accessProviderId, config.getAuthenticatorConfigAlias());
            } else {
                LOG.tracef(
                    "Using access provider '%s' in authenticator config '%s'.",
                    accessProviderId, config.getAuthenticatorConfigAlias());
                return accessProvider;
            }
        }

        final AccessProvider defaultProvider = context.getSession().getProvider(AccessProvider.class);
        if (defaultProvider != null) {
            LOG.debugf(
                "No access provider is configured in authenticator config '%s'. Using server-wide default provider '%s'",
                config.getAuthenticatorConfigAlias(), defaultProvider);
            return defaultProvider;
        }

        LOG.infof(
            "Neither an access provider is configured in authenticator config '%s' nor has a server-wide default provider been set. Using '%s' as a fallback.",
            config.getAuthenticatorConfigAlias(), ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
        return context.getSession().getProvider(AccessProvider.class, ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
    }

    private Response errorResponse(AuthenticationFlowContext context, RestrictClientAuthConfig config) {
        Response response;
        if (MediaTypeMatcher.isHtmlRequest(context.getHttpRequest().getHttpHeaders())) {
            response = htmlErrorResponse(context, config);
        } else {
            response = oAuth2ErrorResponse();
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

    private static Response oAuth2ErrorResponse() {
        return Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
            .entity(new OAuth2ErrorRepresentation(Messages.ACCESS_DENIED, "Access to client is denied."))
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
