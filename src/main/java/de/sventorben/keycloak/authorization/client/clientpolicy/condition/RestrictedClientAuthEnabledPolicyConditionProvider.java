package de.sventorben.keycloak.authorization.client.clientpolicy.condition;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.condition.AbstractClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import static org.keycloak.services.clientpolicy.ClientPolicyVote.ABSTAIN;
import static org.keycloak.services.clientpolicy.ClientPolicyVote.NO;
import static org.keycloak.services.clientpolicy.ClientPolicyVote.YES;

class RestrictedClientAuthEnabledPolicyConditionProvider extends AbstractClientPolicyConditionProvider<ClientPolicyConditionConfigurationRepresentation> {

    static final String PROVIDER_ID = "restrict-client-auth-enabled";

    RestrictedClientAuthEnabledPolicyConditionProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) {
        switch (context.getEvent()) {
            case REGISTER:
            case REGISTERED:
            case UPDATE:
            case UPDATED:
                ClientModel client;
                if (context instanceof ClientCRUDContext) {
                    client = ((ClientCRUDContext) context).getTargetClient();
                } else {
                    client = session.getContext().getClient();
                }
                if (isRestrictedAccessEnabled(client)) {
                    return YES;
                }
                return NO;
            default:
                return ABSTAIN;
        }
    }

    private boolean isRestrictedAccessEnabled(ClientModel client) {
        if (client == null) return false;
        return session.getAllProviders(AccessProvider.class).stream()
            .anyMatch(it -> it.isRestricted(client));
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
}
