package de.sventorben.keycloak.authorization.client.clientpolicy.condition;

import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.services.clientpolicy.condition.AbstractClientPolicyConditionProviderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestrictedClientAuthEnabledPolicyConditionProviderFactory extends AbstractClientPolicyConditionProviderFactory implements ServerInfoAwareProviderFactory {

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        addCommonConfigProperties(CONFIG_PROPERTIES);
    }

    @Override
    public String getHelpText() {
        return "The condition checks whether user authentication on a client has been restricted.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public RestrictedClientAuthEnabledPolicyConditionProvider create(KeycloakSession keycloakSession) {
        return new RestrictedClientAuthEnabledPolicyConditionProvider(keycloakSession);
    }

    @Override
    public String getId() {
        return RestrictedClientAuthEnabledPolicyConditionProvider.PROVIDER_ID;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return OperationalInfo.get();
    }
}
