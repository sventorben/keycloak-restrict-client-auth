package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.access.auto.AutoAccessProviderFactory;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;

/**
 * Checks access whenever a user authenticates, no matter which authentication flow has been used. Since Keycloak
 * runs required actions after the authentication flow, access is checked after other required actions, e.g. updating
 * the password during credential reset.
 */
final class RestrictClientAuthRequiredAction implements RequiredActionProvider {

    private static final String PASSWORD_UPDATE_PENDING = "restrict-client-auth-password-update-pending";
    private static final String UPDATE_PASSWORD = UserModel.RequiredAction.UPDATE_PASSWORD.name();

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
            // triggers are evaluated before any required action runs, i.e. before the password is updated
            if (isPasswordUpdatePending(context)) {
                context.getAuthenticationSession().setAuthNote(PASSWORD_UPDATE_PENDING, Boolean.TRUE.toString());
            }
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
            AutoAccessProviderFactory.PROVIDER_ID, config.getAuthenticatorConfigAlias());
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
        final String errorMessage = isPasswordUpdated(context)
            ? config.getPasswordUpdatedErrorMessage()
            : config.getErrorMessage();
        context.challenge(context.form()
            .setError(errorMessage, context.getUser().getUsername(), client.getClientId())
            .createErrorPage(Response.Status.FORBIDDEN));
    }

    /**
     * Whether the user updated the password during this authentication, e.g. after resetting it.
     */
    private static boolean isPasswordUpdated(RequiredActionContext context) {
        return Boolean.parseBoolean(context.getAuthenticationSession().getAuthNote(PASSWORD_UPDATE_PENDING))
            && !isPasswordUpdatePending(context);
    }

    private static boolean isPasswordUpdatePending(RequiredActionContext context) {
        return context.getAuthenticationSession().getRequiredActions().contains(UPDATE_PASSWORD)
            || context.getUser().getRequiredActionsStream().anyMatch(UPDATE_PASSWORD::equals);
    }

    @Override
    public void close() {
    }

}
