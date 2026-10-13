package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;

import static de.sventorben.keycloak.authorization.client.RestrictClientAuthConfig.ACCESS_PROVIDER_ID;

class EnforceAccessClientPolicyExecutorConfiguration extends ClientPolicyExecutorConfigurationRepresentation {

    static final String CHECK_AUDIENCE = "checkAudience";

    @JsonProperty(ACCESS_PROVIDER_ID)
    private String accessProviderId;

    @JsonProperty(CHECK_AUDIENCE)
    private Boolean checkAudience;

    String getAccessProviderId() {
        return accessProviderId;
    }

    boolean isCheckAudience() {
        return Boolean.TRUE.equals(checkAudience);
    }

    public EnforceAccessClientPolicyExecutorConfiguration parseWithDefaultValues() {
        if (accessProviderId == null) {
            accessProviderId = ClientRoleBasedAccessProviderFactory.PROVIDER_ID;
        }

        return this;
    }
}
