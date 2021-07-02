package de.sventorben.keycloak.authentication;

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.services.messages.Messages;

import java.util.Optional;

final class RestrictClientAuthConfig {

    static final String ERROR_MESSAGE = "restrictClientAuthErrorMessage";

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

}
