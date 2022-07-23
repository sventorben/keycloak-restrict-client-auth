package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.services.messages.Messages;

import java.util.Optional;

public final class RestrictClientAuthConfig {

    static final String ERROR_MESSAGE = "restrictClientAuthErrorMessage";
    public static final String ACCESS_PROVIDER_ID = "accessProviderId";

    private final AuthenticatorConfigModel authenticatorConfigModel;

    RestrictClientAuthConfig(AuthenticatorConfigModel configModel) {
        this.authenticatorConfigModel = configModel;
    }

    String getErrorMessage() {
        return Optional.ofNullable(authenticatorConfigModel)
                .map(AuthenticatorConfigModel::getConfig)
                .map(config -> config.getOrDefault(ERROR_MESSAGE, Messages.ACCESS_DENIED))
                .orElse(Messages.ACCESS_DENIED);
    }

    public String getAccessProviderId() {
        return Optional.ofNullable(authenticatorConfigModel)
            .map(AuthenticatorConfigModel::getConfig)
            .map(config -> config.getOrDefault(ACCESS_PROVIDER_ID, ClientRoleBasedAccessProviderFactory.PROVIDER_ID))
            .orElse(ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
    }

    String getAuthenticatorConfigAlias() {
        return Optional.ofNullable(authenticatorConfigModel)
            .map(AuthenticatorConfigModel::getAlias)
            .orElse(null);
    }
}
