package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;

class EnforceAccessClientPolicyExecutorConfiguration extends ClientPolicyExecutorConfigurationRepresentation {

    static final String CHECK_AUDIENCE = "checkAudience";
    static final String CHECK_SERVICE_ACCOUNTS = "checkServiceAccounts";

    @JsonProperty(CHECK_AUDIENCE)
    private Boolean checkAudience;

    @JsonProperty(CHECK_SERVICE_ACCOUNTS)
    private Boolean checkServiceAccounts;

    boolean isCheckAudience() {
        return Boolean.TRUE.equals(checkAudience);
    }

    boolean isCheckServiceAccounts() {
        return Boolean.TRUE.equals(checkServiceAccounts);
    }
}
