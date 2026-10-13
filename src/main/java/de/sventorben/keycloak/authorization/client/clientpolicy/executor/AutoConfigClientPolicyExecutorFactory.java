package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.access.auto.AutoAccessProviderFactory;
import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
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

import static de.sventorben.keycloak.authorization.client.RestrictClientAuthConfig.ACCESS_PROVIDER_ID;
import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

public class AutoConfigClientPolicyExecutorFactory implements ClientPolicyExecutorProviderFactory, ServerInfoAwareProviderFactory {

    private static final ProviderConfigProperty ACCESS_PROVIDER_ID_PROPERTY = new ProviderConfigProperty(
        ACCESS_PROVIDER_ID,
        "Access Provider",
        "The access provider used to restrict access to clients. The 'auto' access provider is not supported, since " +
            "it cannot decide how to restrict access.",
        LIST_TYPE,
        ClientRoleBasedAccessProviderFactory.PROVIDER_ID,
        false);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
        .property(ACCESS_PROVIDER_ID_PROPERTY)
        .build();

    @Override
    public String getHelpText() {
        return "The executor automatically enables restricted access feature for a client.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession keycloakSession) {
        return new AutoConfigClientPolicyExecutor(keycloakSession, new AccessProviderResolver(keycloakSession));
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        ACCESS_PROVIDER_ID_PROPERTY.setOptions(
            factory.getProviderFactoriesStream(AccessProvider.class)
                .map(ProviderFactory::getId)
                .filter(it -> !AutoAccessProviderFactory.PROVIDER_ID.equals(it))
                .collect(Collectors.toList()));
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return AutoConfigClientPolicyExecutor.PROVIDER_ID;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return OperationalInfo.get();
    }
}
