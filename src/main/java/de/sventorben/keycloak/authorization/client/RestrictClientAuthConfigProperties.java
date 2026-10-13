package de.sventorben.keycloak.authorization.client;

import de.sventorben.keycloak.authorization.client.access.RealmSettings;
import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;

import java.util.List;

import static de.sventorben.keycloak.authorization.client.RestrictClientAuthConfig.ACCESS_PROVIDER_ID;
import static de.sventorben.keycloak.authorization.client.RestrictClientAuthConfig.ERROR_MESSAGE;
import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.ROLE_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

public class RestrictClientAuthConfigProperties {

    private static final ProviderConfigProperty ERROR_MESSAGE_PROPERTY = new ProviderConfigProperty(
            ERROR_MESSAGE,
            "Error message",
            "Error message which will be shown to the user. " +
                "You can directly define a particular message or property, which will be used for mapping the error message e.g. `deny-access-role1`." +
                "If the field is blank, default property 'access-denied' is used.",
            STRING_TYPE,
            Messages.ACCESS_DENIED,
            false);

    public static final ProviderConfigProperty ACCESS_PROVIDER_ID_PROPERTY = new ProviderConfigProperty(
        ACCESS_PROVIDER_ID,
        "Access Provider",
        "The access provider to be used with this authenticator.",
        LIST_TYPE,
        ClientRoleBasedAccessProviderFactory.PROVIDER_ID,
        false);

    private static final ProviderConfigProperty DEFAULT_DENY_PROPERTY = new ProviderConfigProperty(
        RealmSettings.DEFAULT_DENY,
        "Deny access by default",
        "Applies to the whole realm: authenticators, this required action, and client policy executors. If enabled, " +
            "all clients are restricted. Access to clients that are not restricted by the access provider of the " +
            "respective authenticator, required action, or executor is only permitted for users with the default " +
            "access role.",
        BOOLEAN_TYPE,
        false,
        false);

    private static final ProviderConfigProperty DEFAULT_ACCESS_ROLE_PROPERTY = new ProviderConfigProperty(
        RealmSettings.DEFAULT_ACCESS_ROLE,
        "Default access role",
        "If access is denied by default, users with this role may access all clients that are not restricted by " +
            "the access provider. If not set, access to these clients is denied.",
        ROLE_TYPE,
        null,
        false);


    static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property(ERROR_MESSAGE_PROPERTY)
            .property(ACCESS_PROVIDER_ID_PROPERTY)
            .build();

    static final List<ProviderConfigProperty> REALM_SETTINGS_PROPERTIES = ProviderConfigurationBuilder.create()
            .property(ERROR_MESSAGE_PROPERTY)
            .property(ACCESS_PROVIDER_ID_PROPERTY)
            .property(DEFAULT_DENY_PROPERTY)
            .property(DEFAULT_ACCESS_ROLE_PROPERTY)
            .build();

}
