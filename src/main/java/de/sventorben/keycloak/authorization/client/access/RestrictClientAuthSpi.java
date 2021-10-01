package de.sventorben.keycloak.authorization.client.access;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public final class RestrictClientAuthSpi implements Spi {

    private static final String SPI_NAME = "restrict-client-auth-access-provider";

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
        return AccessProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory<AccessProvider>> getProviderFactoryClass() {
        return AccessProviderFactory.class;
    }
}
