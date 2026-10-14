package de.sventorben.keycloak.authorization.client;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

/**
 * The required action is registered and enabled in all realms by default.
 */
class RequiredActions {

    static final String RESTRICT_CLIENT_AUTH = "restrict-client-auth-enforce";

    static void disable(RealmResource realm) {
        AuthenticationManagementResource flows = realm.flows();
        RequiredActionProviderRepresentation requiredAction = flows.getRequiredAction(RESTRICT_CLIENT_AUTH);
        requiredAction.setEnabled(false);
        flows.updateRequiredAction(RESTRICT_CLIENT_AUTH, requiredAction);
    }
}
