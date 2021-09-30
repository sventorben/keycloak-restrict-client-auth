package de.sventorben.keycloak.authentication;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

final class ClientRoleBasedAccess implements Access {

    private static final Logger LOG = Logger.getLogger(ClientRoleBasedAccess.class);

    private final String clientRoleName;

    ClientRoleBasedAccess(String clientRoleName) {
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
        if (!permitted) {
            LOG.warnf("Access for user '%s' is denied. User does not have client role '%s' on client '%s'.",
                    user.getUsername(), clientRoleName, client.getId());
        }
        return permitted;
    }
}
