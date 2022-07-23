package de.sventorben.keycloak.authorization.client.access.policy;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderFactory;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Map;

public final class PolicyBasedAccessProviderFactory implements AccessProviderFactory, ServerInfoAwareProviderFactory {

    private static final String PROVIDER_ID = "policy";

    private Config.Scope config;

    @Override
    public AccessProvider create(KeycloakSession session) {
        return new PolicyBasedAccessProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

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
