package de.sventorben.keycloak.authorization.client.uma;

import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.grants.OAuth2GrantType;
import org.keycloak.protocol.oidc.grants.OAuth2GrantTypeFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Map;

/**
 * Replaces Keycloak's factory for the UMA grant, since Keycloak prefers custom provider factories over its own
 * ones with the same id.
 */
public final class RestrictedPermissionGrantTypeFactory implements OAuth2GrantTypeFactory,
    ServerInfoAwareProviderFactory {

    @Override
    public String getId() {
        return OAuth2Constants.UMA_GRANT_TYPE;
    }

    @Override
    public String getShortcut() {
        return "pg";
    }

    @Override
    public OAuth2GrantType create(KeycloakSession session) {
        return new RestrictedPermissionGrantType();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return OperationalInfo.get();
    }
}
