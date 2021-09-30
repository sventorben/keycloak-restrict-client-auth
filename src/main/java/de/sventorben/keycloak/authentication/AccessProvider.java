package de.sventorben.keycloak.authentication;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface AccessProvider extends Provider {
    boolean isRestricted(ClientModel client);

    boolean isPermitted(ClientModel client, UserModel user);
}
