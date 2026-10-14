package de.sventorben.keycloak.authorization.client;

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.services.messages.Messages;

import java.util.Map;
import java.util.Optional;

public final class RestrictClientAuthConfig {

    static final String ERROR_MESSAGE = "restrictClientAuthErrorMessage";
    static final String PASSWORD_UPDATED_ERROR_MESSAGE = "passwordUpdatedErrorMessage";
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

    /**
     * The error message if access is denied after the user updated the password, or the {@link #getErrorMessage()
     * error message} if none is configured.
     */
    String getPasswordUpdatedErrorMessage() {
        return Optional.ofNullable(config)
            .map(it -> it.get(PASSWORD_UPDATED_ERROR_MESSAGE))
            .filter(it -> !it.isBlank())
            .orElseGet(this::getErrorMessage);
    }

    /**
     * The configured access provider, or {@code null} if none is configured. In this case, the server-wide default
     * access provider is used, see {@link de.sventorben.keycloak.authorization.client.access.AccessProviderResolver}.
     */
    public String getAccessProviderId() {
        return Optional.ofNullable(config)
            .map(it -> it.get(ACCESS_PROVIDER_ID))
            .orElse(null);
    }

    String getAuthenticatorConfigAlias() {
        return alias;
    }
}
