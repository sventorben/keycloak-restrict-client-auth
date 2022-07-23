package de.sventorben.keycloak.authorization.client.access.role;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderFactory;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

public final class ClientRoleBasedAccessProviderFactory implements AccessProviderFactory, ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "client-role";

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
        Map<String, String> operationalInfo = new HashMap<>(OperationalInfo.get());
        operationalInfo.put(CLIENT_ROLE_NAME, getClientRoleName());
        return operationalInfo;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
            .property()
            .name(CLIENT_ROLE_NAME)
            .label("Client role name")
            .defaultValue(CLIENT_ROLE_NAME_DEFAULT)
            .helpText("The name of the client role used to enable the authenticator and grant access.")
            .type(STRING_TYPE)
            .add()
            .build();
    }

    private String getClientRoleName() {
        return config.get(CLIENT_ROLE_NAME, CLIENT_ROLE_NAME_DEFAULT);
    }
}
