package de.sventorben.keycloak.authorization.client.account;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public final class AccountApplicationsFilterSpi implements Spi {

    private static final String SPI_NAME = "restrict-client-auth-account-filter";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AccountApplicationsFilterProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory<AccountApplicationsFilterProvider>> getProviderFactoryClass() {
        return AccountApplicationsFilterProviderFactory.class;
    }
}
