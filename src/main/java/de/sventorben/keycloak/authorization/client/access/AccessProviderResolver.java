package de.sventorben.keycloak.authorization.client.access;

import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

public final class AccessProviderResolver {

    private static final Logger LOG = Logger.getLogger(AccessProviderResolver.class);

    private final KeycloakSession session;

    public AccessProviderResolver(KeycloakSession session) {
        this.session = session;
    }

    public AccessProvider resolve(String accessProviderId, String authenticatorConfigAlias) {
        if (accessProviderId != null) {
            AccessProvider accessProvider = session.getProvider(AccessProvider.class, accessProviderId);
            if (accessProvider == null) {
                LOG.warnf(
                    "Configured access provider '%s' in authenticator config '%s' does not exist.",
                    accessProviderId, authenticatorConfigAlias);
            } else {
                LOG.tracef(
                    "Using access provider '%s' in authenticator config '%s'.",
                    accessProviderId, authenticatorConfigAlias);
                return accessProvider;
            }
        }

        final AccessProvider defaultProvider = session.getProvider(AccessProvider.class);
        if (defaultProvider != null) {
            LOG.debugf(
                "No access provider is configured in authenticator config '%s'. Using server-wide default provider '%s'",
                authenticatorConfigAlias, defaultProvider);
            return defaultProvider;
        }

        LOG.infof(
            "Neither an access provider is configured in authenticator config '%s' nor has a server-wide default provider been set. Using '%s' as a fallback.",
            authenticatorConfigAlias, ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
        return session.getProvider(AccessProvider.class, ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
    }
}
