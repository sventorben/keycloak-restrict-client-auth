package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.access.RealmSettings;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.validate.ValidationError;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class RestrictClientAuthRequiredActionFactory implements RequiredActionFactory, ServerInfoAwareProviderFactory {

    static final String PROVIDER_ID = RealmSettings.REQUIRED_ACTION;

    @Override
    public String getDisplayText() {
        return "Restrict user authentication on clients";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return RestrictClientAuthConfigProperties.REALM_SETTINGS_PROPERTIES;
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, RequiredActionConfigModel model) {
        RequiredActionFactory.super.validateConfig(session, realm, model);
        Map<String, String> config = Optional.ofNullable(model.getConfig()).orElse(Map.of());
        validateAccessProvider(session, config.get(RestrictClientAuthConfig.ACCESS_PROVIDER_ID));
        validateDefaultAccessRole(realm, config.get(RealmSettings.DEFAULT_ACCESS_ROLE));
    }

    private void validateAccessProvider(KeycloakSession session, String accessProviderId) {
        if (accessProviderId != null
            && session.getKeycloakSessionFactory().getProviderFactory(AccessProvider.class, accessProviderId) == null) {
            throw new ValidationException(new ValidationError(getId(), RestrictClientAuthConfig.ACCESS_PROVIDER_ID,
                "Access provider '" + accessProviderId + "' does not exist."));
        }
    }

    private void validateDefaultAccessRole(RealmModel realm, String defaultAccessRole) {
        if (defaultAccessRole != null && KeycloakModelUtils.getRoleFromString(realm, defaultAccessRole) == null) {
            throw new ValidationException(new ValidationError(getId(), RealmSettings.DEFAULT_ACCESS_ROLE,
                "Role '" + defaultAccessRole + "' does not exist."));
        }
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
