package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

import java.util.List;
import java.util.Map;


public class EnforceAccessClientPolicyExecutorFactory implements ClientPolicyExecutorProviderFactory, ServerInfoAwareProviderFactory {

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
        .property()
        .name(EnforceAccessClientPolicyExecutorConfiguration.CHECK_AUDIENCE)
        .label("Check audience")
        .helpText("If enabled, access is also checked for all clients in the audience of the access token. "
            + "Tokens are denied if the user does not have access to any of them.")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .defaultValue(false)
        .add()
        .property()
        .name(EnforceAccessClientPolicyExecutorConfiguration.CHECK_SERVICE_ACCOUNTS)
        .label("Check service accounts")
        .helpText("If enabled, access is also checked for service accounts (client credentials grant): tokens are "
            + "denied if the service account does not have access to any client in the audience of the access "
            + "token, except the client itself.")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .defaultValue(false)
        .add()
        .build();

    @Override
    public String getHelpText() {
        return "The executor denies issuing tokens for users who do not have access to a restricted client, " +
            "e.g. when exchanging an authorization code, refreshing or exchanging a token.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession keycloakSession) {
        return new EnforceAccessClientPolicyExecutor(keycloakSession, new AccessProviderResolver(keycloakSession));
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return EnforceAccessClientPolicyExecutor.PROVIDER_ID;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return OperationalInfo.get();
    }
}
