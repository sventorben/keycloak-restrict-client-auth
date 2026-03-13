package de.sventorben.keycloak.authorization.client.account;

record DefaultAccountApplicationsFilterProvider(AccountApplicationsFilterConfig config) implements AccountApplicationsFilterProvider {

    @Override
    public AccountApplicationsFilterConfig getConfig() {
        return config;
    }

    @Override
    public void close() {
    }
}
