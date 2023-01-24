package de.sventorben.keycloak.authorization.client.access.role;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

public final class ClientRoleBasedAccessProvider implements AccessProvider {

    private static final Logger LOG = Logger.getLogger(ClientRoleBasedAccessProvider.class);

    private final String clientRoleName;

    ClientRoleBasedAccessProvider(String clientRoleName) {
        this.clientRoleName = clientRoleName;
    }

    @Override
    public boolean isRestricted(ClientModel client) {
        return client.getRole(clientRoleName) != null;
    }

    @Override
    public boolean isPermitted(ClientModel client, UserModel user) {
        final RoleModel role = client.getRole(clientRoleName);
        if (role == null) return false;
        if (user == null) return false;
        boolean permitted = user.hasRole(role);
        if (permitted) {
            LOG.debugf(
                "Access for user '%s' to client '%s' in realm '%s' granted.",
                user.getUsername(), client.getClientId(), client.getRealm().getName());
        } else {
            LOG.warnf("Access for user '%s' to client '%s' in realm '%s' is denied. User does not have client role '%s' on client with id '%s'.",
                    user.getUsername(), client.getClientId(), client.getRealm().getName(), clientRoleName, client.getId());
        }
        return permitted;
    }

    @Override
    public void enableFor(ClientModel client) {
        if (isRestricted(client)) return;
        client.addRole(clientRoleName);
    }

    @Override
    public void close() {
    }
}
