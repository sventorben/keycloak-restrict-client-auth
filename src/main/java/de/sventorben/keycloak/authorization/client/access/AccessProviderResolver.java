package de.sventorben.keycloak.authorization.client.access;

import de.sventorben.keycloak.authorization.client.RestrictClientAuthConfig;
import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;

public final class AccessProviderResolver {

    private static final Logger LOG = Logger.getLogger(AccessProviderResolver.class);

    private final RestrictClientAuthConfig config;

    public AccessProviderResolver(RestrictClientAuthConfig config) {
        this.config = config;
    }

    public AccessProvider resolve(AuthenticationFlowContext context) {
        final String accessProviderId = config.getAccessProviderId();

        if (accessProviderId != null) {
            AccessProvider accessProvider = context.getSession().getProvider(AccessProvider.class, accessProviderId);
            if (accessProvider == null) {
                LOG.warnf(
                    "Configured access provider '%s' in authenticator config '%s' does not exist.",
                    accessProviderId, config.getAuthenticatorConfigAlias());
            } else {
                LOG.tracef(
                    "Using access provider '%s' in authenticator config '%s'.",
                    accessProviderId, config.getAuthenticatorConfigAlias());
                return accessProvider;
            }
        }

        final AccessProvider defaultProvider = context.getSession().getProvider(AccessProvider.class);
        if (defaultProvider != null) {
            LOG.debugf(
                "No access provider is configured in authenticator config '%s'. Using server-wide default provider '%s'",
                config.getAuthenticatorConfigAlias(), defaultProvider);
            return defaultProvider;
        }

        LOG.infof(
            "Neither an access provider is configured in authenticator config '%s' nor has a server-wide default provider been set. Using '%s' as a fallback.",
            config.getAuthenticatorConfigAlias(), ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
        return context.getSession().getProvider(AccessProvider.class, ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
    }
}
