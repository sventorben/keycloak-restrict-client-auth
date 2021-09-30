package de.sventorben.keycloak.authentication;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;

interface Access {
    boolean isRestricted(ClientModel client);

    boolean isPermitted(ClientModel client, UserModel user);
}
