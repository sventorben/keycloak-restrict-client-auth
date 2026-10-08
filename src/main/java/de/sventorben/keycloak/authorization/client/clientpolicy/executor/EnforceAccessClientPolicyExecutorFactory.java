package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.RestrictClientAuthConfigProperties;
import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.sventorben.keycloak.authorization.client.RestrictClientAuthConfigProperties.ACCESS_PROVIDER_ID_PROPERTY;

public class EnforceAccessClientPolicyExecutorFactory implements ClientPolicyExecutorProviderFactory, ServerInfoAwareProviderFactory {

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
        .property(ACCESS_PROVIDER_ID_PROPERTY)
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
        RestrictClientAuthConfigProperties.ACCESS_PROVIDER_ID_PROPERTY.setOptions(
            factory.getProviderFactoriesStream(AccessProvider.class)
                .map(ProviderFactory::getId)
                .collect(Collectors.toList()));
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
