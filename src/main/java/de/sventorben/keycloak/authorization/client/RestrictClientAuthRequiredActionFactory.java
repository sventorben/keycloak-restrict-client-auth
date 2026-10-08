package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RestrictClientAuthRequiredActionFactory implements RequiredActionFactory, ServerInfoAwareProviderFactory {

    static final String PROVIDER_ID = "restrict-client-auth-enforce";

    @Override
    public String getDisplayText() {
        return "Restrict user authentication on clients";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return RestrictClientAuthConfigProperties.CONFIG_PROPERTIES;
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new RestrictClientAuthRequiredAction(new AccessProviderResolver(session));
    }

    @Override
    public void init(Config.Scope config) {
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
        return PROVIDER_ID;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return OperationalInfo.get();
    }
}
