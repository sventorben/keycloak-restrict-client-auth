package de.sventorben.keycloak.authentication;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;

import java.util.List;

import static de.sventorben.keycloak.authentication.RestrictClientAuthConfig.ERROR_MESSAGE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

public class RestrictClientAuthConfigProperties {

    private static final ProviderConfigProperty ERROR_MESSAGE_PROPERTY = new ProviderConfigProperty(
            ERROR_MESSAGE,
            "Error message",
            "Error message which will be shown to the user. " +
                "You can directly define particular message or property, which will be used for mapping the error message f.e `deny-access-role1`." +
                "If the field is blank, default property 'access-denied' is used.",
            STRING_TYPE,
            Messages.ACCESS_DENIED,
            false);

    static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property(ERROR_MESSAGE_PROPERTY)
            .build();

}