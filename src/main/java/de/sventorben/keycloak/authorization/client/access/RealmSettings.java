package de.sventorben.keycloak.authorization.client.access;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;

import java.util.Map;
import java.util.Optional;

/**
 * Realm-wide settings, configured via the configuration of the required action. They apply to all enforcement points
 * of the realm: authenticators, the required action, and client policy executors.
 */
public final class RealmSettings {

    public static final String REQUIRED_ACTION = "restrict-client-auth-enforce";

    public static final String DEFAULT_DENY = "defaultDeny";
    public static final String DEFAULT_ACCESS_ROLE = "defaultAccessRole";

    private final Map<String, String> config;

    private RealmSettings(Map<String, String> config) {
        this.config = config;
    }

    public static RealmSettings of(RealmModel realm) {
        return new RealmSettings(Optional.ofNullable(realm)
            .map(it -> it.getRequiredActionConfigByAlias(REQUIRED_ACTION))
            .map(RequiredActionConfigModel::getConfig)
            .orElse(Map.of()));
    }

    boolean isDefaultDeny() {
        return Boolean.parseBoolean(config.get(DEFAULT_DENY));
    }

    /**
     * The role that grants access to clients not restricted by the access provider, if access is denied by default.
     */
    String getDefaultAccessRole() {
        return config.get(DEFAULT_ACCESS_ROLE);
    }
}
