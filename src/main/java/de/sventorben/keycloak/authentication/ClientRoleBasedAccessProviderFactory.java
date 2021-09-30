package de.sventorben.keycloak.authentication;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Map;

public final class ClientRoleBasedAccessProviderFactory implements AccessProviderFactory, ServerInfoAwareProviderFactory {

    static final String PROVIDER_ID = "restrict-client-auth-access-client-role";

    private static final String CLIENT_ROLE_NAME = "clientRoleName";
    private static final String CLIENT_ROLE_NAME_DEFAULT = "restricted-access";

    private Config.Scope config;

    @Override
    public AccessProvider create(KeycloakSession session) {
        String clientRoleName = getClientRoleName();
        return new ClientRoleBasedAccessProvider(clientRoleName);
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
        String version = getClass().getPackage().getImplementationVersion();
        return Map.of("Version", version, CLIENT_ROLE_NAME, getClientRoleName());
    }

    private String getClientRoleName() {
        return config.get(CLIENT_ROLE_NAME, CLIENT_ROLE_NAME_DEFAULT);
    }
}
