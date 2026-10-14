package de.sventorben.keycloak.authorization.client.access;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Denies access by default: all clients are restricted. Access to clients that are restricted by the given access
 * provider is decided by this access provider. Access to all other clients is only permitted for users with the
 * default access role.
 */
final class DefaultDenyAccessProvider implements AccessProvider {

    private static final Logger LOG = Logger.getLogger(DefaultDenyAccessProvider.class);

    private static final AtomicBoolean ADMIN_REALM_WARNING_LOGGED = new AtomicBoolean();

    private final AccessProvider accessProvider;
    private final String defaultAccessRole;

    /**
     * @param accessProvider    the access provider to decide access to the clients it restricts, or {@code null} if
     *                          no client is restricted by an access provider
     * @param defaultAccessRole the role that grants access to all other clients, or {@code null} to deny access
     */
    DefaultDenyAccessProvider(AccessProvider accessProvider, String defaultAccessRole) {
        this.accessProvider = accessProvider;
        this.defaultAccessRole = defaultAccessRole;
    }

    @Override
    public boolean isRestricted(ClientModel client) {
        warnIfAdminRealm(client);
        return true;
    }

    @Override
    public boolean isPermitted(ClientModel client, UserModel user) {
        if (accessProvider != null && accessProvider.isRestricted(client)) {
            return accessProvider.isPermitted(client, user);
        }
        return hasDefaultAccessRole(client, user);
    }

    @Override
    public void enableFor(ClientModel client) {
        if (accessProvider != null) {
            accessProvider.enableFor(client);
        }
    }

    @Override
    public void close() {
    }

    private boolean hasDefaultAccessRole(ClientModel client, UserModel user) {
        if (defaultAccessRole == null || user == null) {
            LOG.debugf("Access to client '%s' in realm '%s' is denied by default.",
                client.getClientId(), client.getRealm().getName());
            return false;
        }
        RoleModel role = KeycloakModelUtils.getRoleFromString(client.getRealm(), defaultAccessRole);
        if (role == null) {
            LOG.warnf("Access for user '%s' to client '%s' is denied. Default access role '%s' does not exist in "
                    + "realm '%s'.",
                user.getUsername(), client.getClientId(), defaultAccessRole, client.getRealm().getName());
            return false;
        }
        boolean permitted = user.hasRole(role);
        if (!permitted) {
            LOG.debugf("Access for user '%s' to client '%s' in realm '%s' is denied. User does not have default access "
                    + "role '%s'.",
                user.getUsername(), client.getClientId(), client.getRealm().getName(), defaultAccessRole);
        }
        return permitted;
    }

    private static void warnIfAdminRealm(ClientModel client) {
        if (Config.getAdminRealm().equals(client.getRealm().getName())
            && ADMIN_REALM_WARNING_LOGGED.compareAndSet(false, true)) {
            LOG.warnf("Access is denied by default in admin realm '%s'. Make sure administrators can still access "
                + "the admin console and admin REST API.", client.getRealm().getName());
        }
    }
}
