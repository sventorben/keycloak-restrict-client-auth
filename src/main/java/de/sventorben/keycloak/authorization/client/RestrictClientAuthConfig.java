package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.services.messages.Messages;

import java.util.Map;
import java.util.Optional;

public final class RestrictClientAuthConfig {

    static final String ERROR_MESSAGE = "restrictClientAuthErrorMessage";
    public static final String ACCESS_PROVIDER_ID = "accessProviderId";

    private final Map<String, String> config;
    private final String alias;

    RestrictClientAuthConfig(AuthenticatorConfigModel configModel) {
        this.config = Optional.ofNullable(configModel).map(AuthenticatorConfigModel::getConfig).orElse(null);
        this.alias = Optional.ofNullable(configModel).map(AuthenticatorConfigModel::getAlias).orElse(null);
    }

    RestrictClientAuthConfig(RequiredActionConfigModel configModel) {
        this.config = Optional.ofNullable(configModel).map(RequiredActionConfigModel::getConfig).orElse(null);
        this.alias = Optional.ofNullable(configModel).map(RequiredActionConfigModel::getAlias).orElse(null);
    }

    String getErrorMessage() {
        return Optional.ofNullable(config)
                .map(it -> it.getOrDefault(ERROR_MESSAGE, Messages.ACCESS_DENIED))
                .orElse(Messages.ACCESS_DENIED);
    }

    public String getAccessProviderId() {
        return Optional.ofNullable(config)
            .map(it -> it.getOrDefault(ACCESS_PROVIDER_ID, ClientRoleBasedAccessProviderFactory.PROVIDER_ID))
            .orElse(ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
    }

    String getAuthenticatorConfigAlias() {
        return alias;
    }
}
