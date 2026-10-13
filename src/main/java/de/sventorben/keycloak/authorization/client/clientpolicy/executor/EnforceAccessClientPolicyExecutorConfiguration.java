package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;

class EnforceAccessClientPolicyExecutorConfiguration extends ClientPolicyExecutorConfigurationRepresentation {

    static final String CHECK_AUDIENCE = "checkAudience";

    @JsonProperty(CHECK_AUDIENCE)
    private Boolean checkAudience;

    boolean isCheckAudience() {
        return Boolean.TRUE.equals(checkAudience);
    }
}
