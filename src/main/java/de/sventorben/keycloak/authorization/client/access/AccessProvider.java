package de.sventorben.keycloak.authorization.client.access;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface AccessProvider extends Provider {
    boolean isRestricted(ClientModel client);

    boolean isPermitted(ClientModel client, UserModel user);

    void enableFor(ClientModel client);
}
