package de.sventorben.keycloak.authorization.client;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ResourcePermissionsResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

import java.util.Set;

import static de.sventorben.keycloak.authorization.client.TestConstants.CLIENT_TEST_RESTRICTED_BY_POLICY;
import static de.sventorben.keycloak.authorization.client.TestConstants.REALM_TEST;
import static de.sventorben.keycloak.authorization.client.TestConstants.USER_TEST_RESTRICTED;

/**
 * Changes the permission for the client resource of the client that is restricted by policy.
 */
class ClientResourcePermission {

    private static final String RESOURCE_CLIENT = "Keycloak Client Resource";
    private static final String PERMISSION_CLIENT_RESOURCE = "Access Keycloak Client Resource";
    private static final String POLICY_REALM_ROLE = "Realm Role restricted-access-via-policy";
    private static final String POLICY_DENYING_UNRESTRICTED_USER = "Only user test-restricted";

    private final AuthorizationResource authorization;

    ClientResourcePermission(Keycloak admin) {
        RealmResource testRealm = admin.realm(REALM_TEST);
        String clientId = testRealm.clients().findByClientId(CLIENT_TEST_RESTRICTED_BY_POLICY).get(0).getId();
        this.authorization = testRealm.clients().get(clientId).authorization();
    }

    /**
     * Adds a policy to the permission that denies access to all users but the restricted test user.
     */
    void addDenyingPolicy(DecisionStrategy decisionStrategy) {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName(POLICY_DENYING_UNRESTRICTED_USER);
        policy.addUser(USER_TEST_RESTRICTED);
        authorization.policies().user().create(policy).close();
        update(decisionStrategy, Set.of(POLICY_REALM_ROLE, POLICY_DENYING_UNRESTRICTED_USER));
    }

    void removeDenyingPolicy() {
        update(DecisionStrategy.UNANIMOUS, Set.of(POLICY_REALM_ROLE));
        String policyId = authorization.policies().user().findByName(POLICY_DENYING_UNRESTRICTED_USER).getId();
        authorization.policies().policy(policyId).remove();
    }

    private void update(DecisionStrategy decisionStrategy, Set<String> policies) {
        ResourcePermissionsResource permissions = authorization.permissions().resource();
        ResourcePermissionRepresentation permission = permissions.findByName(PERMISSION_CLIENT_RESOURCE);
        permission.setDecisionStrategy(decisionStrategy);
        permission.setResources(Set.of(RESOURCE_CLIENT));
        permission.setPolicies(policies);
        permissions.findById(permission.getId()).update(permission);
    }
}
