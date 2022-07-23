package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.DISABLED;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

public final class RestrictClientAuthAuthenticatorFactory implements AuthenticatorFactory, ServerInfoAwareProviderFactory {

    private static final Logger LOG = Logger.getLogger(RestrictClientAuthAuthenticatorFactory.class);

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{REQUIRED, DISABLED};

    private static final String PROVIDER_ID = "restrict-client-auth-authenticator";

    private Config.Scope config;

    @Override
    public String getDisplayType() {
        return "Restrict user authentication on clients";
    }

    @Override
    public String getReferenceCategory() {
        return "Auhtorization";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Restricts user authentication on clients based on an access provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return RestrictClientAuthConfigProperties.CONFIG_PROPERTIES;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new RestrictClientAuthAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
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
