package de.sventorben.keycloak.authorization.client.account;

import org.keycloak.provider.Provider;

public interface AccountApplicationsFilterProvider extends Provider {

    AccountApplicationsFilterConfig getConfig();
}
