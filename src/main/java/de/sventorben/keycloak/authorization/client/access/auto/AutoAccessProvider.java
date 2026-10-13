package de.sventorben.keycloak.authorization.client.access.auto;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Determines the access providers per client: a client is restricted if any other access provider restricts it, and
 * access is permitted only if all access providers that restrict the client permit it. Considers all installed access
 * providers, including custom ones.
 */
public final class AutoAccessProvider implements AccessProvider {

    private static final Logger LOG = Logger.getLogger(AutoAccessProvider.class);

    private final KeycloakSession keycloakSession;

    AutoAccessProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public boolean isRestricted(ClientModel client) {
        return otherAccessProviders().stream().anyMatch(it -> it.isRestricted(client));
    }

    @Override
    public boolean isPermitted(ClientModel client, UserModel user) {
        List<AccessProvider> restrictingAccessProviders = otherAccessProviders().stream()
            .filter(it -> it.isRestricted(client))
            .collect(Collectors.toList());
        return !restrictingAccessProviders.isEmpty()
            && restrictingAccessProviders.stream().allMatch(it -> it.isPermitted(client, user));
    }

    @Override
    public void enableFor(ClientModel client) {
        LOG.warnf("Cannot restrict access to client '%s' in realm '%s' automatically. Configure a specific access "
            + "provider to restrict access to clients.", client.getClientId(), client.getRealm().getName());
    }

    @Override
    public void close() {
    }

    private List<AccessProvider> otherAccessProviders() {
        return keycloakSession.getAllProviders(AccessProvider.class).stream()
            .filter(it -> !(it instanceof AutoAccessProvider))
            .collect(Collectors.toList());
    }
}
