package de.sventorben.keycloak.authorization.client.access;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * Decides which clients are restricted and which users may access them.
 */
public interface AccessProvider extends Provider {

    /**
     * Whether this access provider restricts access to the given client, e.g. because the client has a certain client
     * role or resource.
     */
    boolean isRestricted(ClientModel client);

    /**
     * Whether the given user may access the given client. Callers only ask for clients this access provider
     * {@link #isRestricted(ClientModel) restricts}; access to other clients is decided by the caller. For clients this
     * access provider does not restrict, implementations deny access, so that access is never granted by mistake,
     * e.g. if the restriction has been removed meanwhile.
     */
    boolean isPermitted(ClientModel client, UserModel user);

    /**
     * Restricts access to the given client, e.g. by adding a client role or resource.
     */
    void enableFor(ClientModel client);
}
