package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;

/**
 * Checks access whenever a user authenticates, no matter which authentication flow has been used. Since Keycloak
 * runs required actions after the authentication flow, access is checked after other required actions, e.g. updating
 * the password during credential reset.
 */
final class RestrictClientAuthRequiredAction implements RequiredActionProvider {

    private final AccessProviderResolver accessProviderResolver;

    RestrictClientAuthRequiredAction(AccessProviderResolver accessProviderResolver) {
        this.accessProviderResolver = accessProviderResolver;
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.NOT_SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (!isPermitted(context)) {
            context.getAuthenticationSession().addRequiredAction(RestrictClientAuthRequiredActionFactory.PROVIDER_ID);
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        if (isPermitted(context)) {
            context.success();
        } else {
            deny(context);
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        deny(context);
    }

    private boolean isPermitted(RequiredActionContext context) {
        final RestrictClientAuthConfig config = new RestrictClientAuthConfig(context.getConfig());
        final AccessProvider access = accessProviderResolver.resolve(
            config.getAccessProviderId(), config.getAuthenticatorConfigAlias());
        final ClientModel client = context.getAuthenticationSession().getClient();
        return !access.isRestricted(client) || access.isPermitted(client, context.getUser());
    }

    private void deny(RequiredActionContext context) {
        final RestrictClientAuthConfig config = new RestrictClientAuthConfig(context.getConfig());
        final ClientModel client = context.getAuthenticationSession().getClient();
        context.getEvent()
            .realm(context.getRealm())
            .client(client)
            .user(context.getUser())
            .error(Errors.ACCESS_DENIED);
        context.challenge(context.form()
            .setError(config.getErrorMessage(), context.getUser().getUsername(), client.getClientId())
            .createErrorPage(Response.Status.FORBIDDEN));
    }

    @Override
    public void close() {
    }

}
