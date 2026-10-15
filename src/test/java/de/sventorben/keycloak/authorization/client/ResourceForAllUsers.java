package de.sventorben.keycloak.authorization.client;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

import java.util.Set;

import static de.sventorben.keycloak.authorization.client.TestConstants.CLIENT_TEST_RESTRICTED_BY_POLICY;

/**
 * Adds a resource that all users may access to the client restricted by policy, i.e. a resource server.
 */
class ResourceForAllUsers {

    private static final String RESOURCE_DATA = "data";

    /**
     * @return the id of the resource
     */
    static String add(RealmResource realm) {
        String clientId = realm.clients().findByClientId(CLIENT_TEST_RESTRICTED_BY_POLICY).get(0).getId();
        AuthorizationResource authorization = realm.clients().get(clientId).authorization();

        ResourceRepresentation resource = new ResourceRepresentation(RESOURCE_DATA);
        authorization.resources().create(resource).close();

        RolePolicyRepresentation policy = new RolePolicyRepresentation();
        policy.setName("All users");
        policy.addRole("default-roles-test-realm");
        authorization.policies().role().create(policy).close();

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        permission.setName("Access data");
        permission.setResources(Set.of(RESOURCE_DATA));
        permission.setPolicies(Set.of("All users"));
        authorization.permissions().resource().create(permission).close();

        return authorization.resources().findByName(RESOURCE_DATA).get(0).getId();
    }
}
