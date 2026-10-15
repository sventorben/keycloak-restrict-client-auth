package de.sventorben.keycloak.authorization.client.access;

import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;

public final class AccessProviderResolver {

    private static final Logger LOG = Logger.getLogger(AccessProviderResolver.class);


    private final KeycloakSession session;

    public AccessProviderResolver(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Resolves the access provider for an enforcement point, taking the {@link RealmSettings realm settings} of the
     * current realm into account. If the realm settings deny access by default, the access provider denies access to
     * all clients it does not restrict, unless the user has the default access role.
     */
    public AccessProvider resolve(String accessProviderId, String configAlias) {
        RealmModel realm = Optional.ofNullable(session.getContext()).map(KeycloakContext::getRealm).orElse(null);
        RealmSettings settings = RealmSettings.of(realm);
        AccessProvider accessProvider = resolveConfigured(accessProviderId, configAlias);
        if (settings.isDefaultDeny() && !(accessProvider instanceof DenyingAccessProvider)) {
            return new DefaultDenyAccessProvider(accessProvider, settings.getDefaultAccessRole());
        }
        return accessProvider;
    }

    private AccessProvider resolveConfigured(String accessProviderId, String authenticatorConfigAlias) {
        if (accessProviderId != null) {
            AccessProvider accessProvider = session.getProvider(AccessProvider.class, accessProviderId);
            if (accessProvider == null) {
                LOG.warnf(
                    "Configured access provider '%s' in config '%s' does not exist. Access is denied to all clients.",
                    accessProviderId, authenticatorConfigAlias);
                return new DenyingAccessProvider();
            }
            LOG.tracef(
                "Using access provider '%s' in authenticator config '%s'.",
                accessProviderId, authenticatorConfigAlias);
            return accessProvider;
        }

        final AccessProvider defaultProvider = session.getProvider(AccessProvider.class);
        if (defaultProvider != null) {
            LOG.debugf(
                "No access provider is configured in authenticator config '%s'. Using server-wide default provider '%s'",
                authenticatorConfigAlias, defaultProvider);
            return defaultProvider;
        }

        LOG.debugf(
            "Neither an access provider is configured in authenticator config '%s' nor has a server-wide default provider been set. Using '%s' as a fallback.",
            authenticatorConfigAlias, ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
        return session.getProvider(AccessProvider.class, ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
    }

    /**
     * Denies access to all clients, so that access is never granted by mistake if the configured access provider does
     * not exist, e.g. because of a typo or because a custom access provider has been removed.
     */
    private static final class DenyingAccessProvider implements AccessProvider {

        @Override
        public boolean isRestricted(ClientModel client) {
            return true;
        }

        @Override
        public boolean isPermitted(ClientModel client, UserModel user) {
            return false;
        }

        @Override
        public void enableFor(ClientModel client) {
        }

        @Override
        public void close() {
        }
    }

}
