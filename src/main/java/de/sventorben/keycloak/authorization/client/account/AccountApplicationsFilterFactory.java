package de.sventorben.keycloak.authorization.client.account;

import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;

public final class AccountApplicationsFilterFactory implements AccountApplicationsFilterProviderFactory, ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "default";

    private static final String FILTER_DYNAMIC_APPS = "filterDynamicApps";
    private static final boolean FILTER_DYNAMIC_APPS_DEFAULT = true;

    private static final String FILTER_ALWAYS_DISPLAY_APPS = "filterAlwaysDisplayApps";
    private static final boolean FILTER_ALWAYS_DISPLAY_APPS_DEFAULT = true;

    private Config.Scope config;

    @Override
    public AccountApplicationsFilterProvider create(KeycloakSession session) {
        AccountApplicationsFilterConfig filterConfig = new AccountApplicationsFilterConfig(
            isFilterDynamicApps(),
            isFilterAlwaysDisplayApps()
        );
        return new DefaultAccountApplicationsFilterProvider(filterConfig);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
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
        Map<String, String> operationalInfo = new HashMap<>(OperationalInfo.get());
        operationalInfo.put(FILTER_DYNAMIC_APPS, String.valueOf(isFilterDynamicApps()));
        operationalInfo.put(FILTER_ALWAYS_DISPLAY_APPS, String.valueOf(isFilterAlwaysDisplayApps()));
        return operationalInfo;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
            .property()
            .name(FILTER_DYNAMIC_APPS)
            .label("Filter dynamic apps")
            .defaultValue(FILTER_DYNAMIC_APPS_DEFAULT)
            .helpText("Filter applications that appear dynamically (via sessions/consents) for restricted clients the user cannot access.")
            .type(BOOLEAN_TYPE)
            .add()
            .property()
            .name(FILTER_ALWAYS_DISPLAY_APPS)
            .label("Filter always-display apps")
            .defaultValue(FILTER_ALWAYS_DISPLAY_APPS_DEFAULT)
            .helpText("Filter applications marked as 'always display in console' for restricted clients the user cannot access.")
            .type(BOOLEAN_TYPE)
            .add()
            .build();
    }

    private boolean isFilterDynamicApps() {
        return config.getBoolean(FILTER_DYNAMIC_APPS, FILTER_DYNAMIC_APPS_DEFAULT);
    }

    private boolean isFilterAlwaysDisplayApps() {
        return config.getBoolean(FILTER_ALWAYS_DISPLAY_APPS, FILTER_ALWAYS_DISPLAY_APPS_DEFAULT);
    }
}
