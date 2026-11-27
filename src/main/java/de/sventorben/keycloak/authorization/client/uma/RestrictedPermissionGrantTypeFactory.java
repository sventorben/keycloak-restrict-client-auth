package de.sventorben.keycloak.authorization.client.uma;

import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.grants.OAuth2GrantType;
import org.keycloak.protocol.oidc.grants.OAuth2GrantTypeFactory;
import org.keycloak.protocol.oidc.grants.OAuth2GrantTypeSpi;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Map;

/**
 * Replaces Keycloak's factory for the UMA grant, since Keycloak prefers custom provider factories over its own
 * ones with the same id and order. If disabled via {@code spi-oauth2-grant-type--restrict-client-auth--enabled=false},
 * the factory has a lower order than Keycloak's, so that Keycloak uses its own factory.
 */
public final class RestrictedPermissionGrantTypeFactory implements OAuth2GrantTypeFactory,
    ServerInfoAwareProviderFactory {

    private static final String CONFIG_SCOPE = "restrict-client-auth";

    @Override
    public String getId() {
        return OAuth2Constants.UMA_GRANT_TYPE;
    }

    @Override
    public String getShortcut() {
        return "pg";
    }

    /**
     * Keycloak compares the order of factories with the same id before it initializes them, so the configuration is
     * read here and not in {@link #init(Config.Scope)}.
     */
    @Override
    public int order() {
        boolean enabled = Config.scope(OAuth2GrantTypeSpi.SPI_NAME, CONFIG_SCOPE).getBoolean("enabled", true);
        return enabled ? 0 : -1;
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
