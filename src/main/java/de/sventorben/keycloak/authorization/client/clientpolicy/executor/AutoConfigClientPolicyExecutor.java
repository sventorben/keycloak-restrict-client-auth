package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

import java.util.Objects;

class AutoConfigClientPolicyExecutor implements ClientPolicyExecutorProvider<AutoConfigClientPolicyExecutorConfiguration> {

    static final String PROVIDER_ID = "restrict-client-auth-auto-config";

    private final KeycloakSession keycloakSession;
    private final AccessProviderResolver accessProviderResolver;
    private AccessProvider accessProvider;

    AutoConfigClientPolicyExecutor(KeycloakSession keycloakSession, AccessProviderResolver accessProviderResolver) {
        this.keycloakSession = keycloakSession;
        this.accessProviderResolver = accessProviderResolver;
    }

    @Override
    public Class<AutoConfigClientPolicyExecutorConfiguration> getExecutorConfigurationClass() {
        return AutoConfigClientPolicyExecutorConfiguration.class;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) {
        switch (context.getEvent()) {
            case REGISTER:
            case REGISTERED:
            case UPDATE:
            case UPDATED:
                ClientModel client;
                if (context instanceof ClientCRUDContext) {
                    client = ((ClientCRUDContext) context).getTargetClient();
                } else {
                    client = keycloakSession.getContext().getClient();
                }
                enable(client);
                break;
        }
    }

    private void enable(ClientModel client) {
        accessProvider.enableFor(client);
    }

    @Override
    public void setupConfiguration(AutoConfigClientPolicyExecutorConfiguration config) {
        AutoConfigClientPolicyExecutorConfiguration configuration = Objects.requireNonNullElseGet(config,
            AutoConfigClientPolicyExecutorConfiguration::new).parseWithDefaultValues();
        accessProvider = accessProviderResolver.resolve(configuration.getAccessProviderId(), PROVIDER_ID);
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
}
