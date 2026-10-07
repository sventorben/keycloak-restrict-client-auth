package de.sventorben.keycloak.authorization.client;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.json.RawJsonValue;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;

import java.util.List;
import java.util.Map;

class EnforceAccessPolicy {

    static void enable(RealmResource realm) {
        ClientPolicyExecutorRepresentation executor = new ClientPolicyExecutorRepresentation();
        executor.setExecutorProviderId("restrict-client-auth-enforce");
        executor.setConfiguration(RawJsonValue.of(Map.of("accessProviderId", "client-role")));
        ClientProfileRepresentation profile = new ClientProfileRepresentation();
        profile.setName("Enforce restricted access");
        profile.setExecutors(List.of(executor));
        ClientProfilesRepresentation profiles = realm.clientPoliciesProfilesResource().getProfiles(false);
        profiles.getProfiles().add(profile);
        realm.clientPoliciesProfilesResource().updateProfiles(profiles);

        ClientPolicyConditionRepresentation condition = new ClientPolicyConditionRepresentation();
        condition.setConditionProviderId("any-client");
        condition.setConfiguration(RawJsonValue.of(Map.of()));
        ClientPolicyRepresentation policy = new ClientPolicyRepresentation();
        policy.setName("enforce-restricted-access-all-clients");
        policy.setEnabled(true);
        policy.setConditions(List.of(condition));
        policy.setProfiles(List.of(profile.getName()));
        ClientPoliciesRepresentation policies = realm.clientPoliciesPoliciesResource().getPolicies();
        policies.getPolicies().add(policy);
        realm.clientPoliciesPoliciesResource().updatePolicies(policies);
    }
}
