package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.sventorben.keycloak.authorization.client.access.auto.AutoAccessProviderFactory;
import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;

import static de.sventorben.keycloak.authorization.client.RestrictClientAuthConfig.ACCESS_PROVIDER_ID;

class AutoConfigClientPolicyExecutorConfiguration extends ClientPolicyExecutorConfigurationRepresentation {

    @JsonProperty(ACCESS_PROVIDER_ID)
    private String accessProviderId;

    String getAccessProviderId() {
        return accessProviderId;
    }

    /**
     * The 'auto' access provider cannot decide how to restrict access to a client.
     */
    @Override
    public boolean validateConfig() {
        return !AutoAccessProviderFactory.PROVIDER_ID.equals(accessProviderId);
    }

    public AutoConfigClientPolicyExecutorConfiguration parseWithDefaultValues() {
        if (accessProviderId == null) {
            accessProviderId = ClientRoleBasedAccessProviderFactory.PROVIDER_ID;
        }

        return this;
    }
}
