package de.sventorben.keycloak.authorization.client.account;

public record AccountApplicationsFilterConfig(boolean filterDynamicApps, boolean filterAlwaysDisplayApps) {

    public boolean shouldFilterDynamicApps() {
        return filterDynamicApps;
    }

    public boolean shouldFilterAlwaysDisplayApps() {
        return filterAlwaysDisplayApps;
    }

    public boolean isFilteringEnabled() {
        return filterDynamicApps || filterAlwaysDisplayApps;
    }
}
