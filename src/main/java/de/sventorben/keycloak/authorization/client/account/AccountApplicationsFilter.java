package de.sventorben.keycloak.authorization.client.account;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.role.ClientRoleBasedAccessProviderFactory;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Provider
public class AccountApplicationsFilter {

    private static final Logger LOG = Logger.getLogger(AccountApplicationsFilter.class);

    @ServerResponseFilter
    @SuppressWarnings({"deprecation", "unused"}) // deprecation: Resteasy.getContextData has no simple alternative; unused: method invoked by JAX-RS at runtime
    public void filterApplications(ContainerRequestContext request,
                                   ContainerResponseContext response) {
        String path = request.getUriInfo().getPath();
        if (!path.endsWith("/account/applications")) {
            return;
        }

        LOG.tracef("Intercepted request to '%s', applying account applications filter", path);

        KeycloakSession session = Resteasy.getContextData(KeycloakSession.class);
        if (session == null) {
            LOG.warn("Could not obtain KeycloakSession from context, skipping filtering");
            return;
        }

        AccountApplicationsFilterProvider filterProvider = getFilterProvider(session);
        if (filterProvider == null) {
            LOG.debug("No AccountApplicationsFilterProvider found, skipping filtering");
            return;
        }

        AccountApplicationsFilterConfig config = filterProvider.getConfig();
        if (!config.isFilteringEnabled()) {
            LOG.debugf("Account applications filtering is disabled (filterDynamicApps=%s, filterAlwaysDisplayApps=%s)",
                config.shouldFilterDynamicApps(), config.shouldFilterAlwaysDisplayApps());
            return;
        }

        LOG.tracef("Filter config: filterDynamicApps=%s, filterAlwaysDisplayApps=%s",
            config.shouldFilterDynamicApps(), config.shouldFilterAlwaysDisplayApps());

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
            .setRealm(session.getContext().getRealm())
            .setConnection(session.getContext().getConnection())
            .setHeaders(session.getContext().getRequestHeaders())
            .authenticate();

        if (authResult == null) {
            LOG.debug("No authenticated user found, skipping filtering");
            return;
        }

        FilterContext ctx = new FilterContext(
            session.getContext().getRealm(),
            authResult.getUser(),
            getAccessProvider(session),
            config);

        LOG.debugf("Filtering account applications for user '%s' in realm '%s'",
            ctx.user.getUsername(), ctx.realm.getName());

        filterResponseEntity(response, ctx);
    }

    private void filterResponseEntity(ContainerResponseContext response, FilterContext ctx) {
        Object entity = response.getEntity();
        if (entity instanceof Stream) {
            @SuppressWarnings("unchecked")
            Stream<ClientRepresentation> clientStream = (Stream<ClientRepresentation>) entity;
            response.setEntity(filterClients(clientStream, ctx));
        } else if (entity instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<ClientRepresentation> clients = (Collection<ClientRepresentation>) entity;
            int originalCount = clients.size();
            List<ClientRepresentation> filtered = filterClients(clients.stream(), ctx);
            logFilteringResult(ctx, originalCount, filtered.size());
            response.setEntity(filtered);
        }
    }

    private void logFilteringResult(FilterContext ctx, int originalCount, int filteredCount) {
        int removed = originalCount - filteredCount;
        if (removed > 0) {
            LOG.infof("Filtered %d of %d applications for user '%s' in realm '%s'",
                removed, originalCount, ctx.user.getUsername(), ctx.realm.getName());
        } else {
            LOG.debugf("No applications filtered for user '%s' in realm '%s' (showing all %d)",
                ctx.user.getUsername(), ctx.realm.getName(), originalCount);
        }
    }

    private List<ClientRepresentation> filterClients(Stream<ClientRepresentation> clients,
                                                      FilterContext ctx) {
        return clients
            .filter(clientRep -> shouldShowApplication(clientRep, ctx))
            .collect(Collectors.toList());
    }

    private boolean shouldShowApplication(ClientRepresentation clientRep,
                                          FilterContext ctx) {
        ClientModel client = ctx.realm.getClientByClientId(clientRep.getClientId());
        if (client == null) {
            LOG.debugf("Client '%s' not found in realm, showing in console", clientRep.getClientId());
            return true;
        }

        if (!ctx.accessProvider.isRestricted(client)) {
            LOG.tracef("Client '%s' is not restricted, showing in console for user '%s'",
                clientRep.getClientId(), ctx.user.getUsername());
            return true;
        }

        boolean isAlwaysDisplay = client.isAlwaysDisplayInConsole();
        boolean shouldFilter = isAlwaysDisplay
            ? ctx.config.shouldFilterAlwaysDisplayApps()
            : ctx.config.shouldFilterDynamicApps();

        if (!shouldFilter) {
            String clientType = isAlwaysDisplay ? "always-display" : "dynamic";
            LOG.tracef("Filtering disabled for %s clients, showing restricted client '%s' for user '%s'",
                clientType, clientRep.getClientId(), ctx.user.getUsername());
            return true;
        }

        boolean hasPermission = ctx.accessProvider.isPermitted(client, ctx.user);
        if (hasPermission) {
            LOG.debugf("User '%s' has permission to access restricted client '%s' in realm '%s', showing in console",
                ctx.user.getUsername(), clientRep.getClientId(), ctx.realm.getName());
        } else {
            String clientType = isAlwaysDisplay ? "always-display" : "dynamic";
            LOG.warnf("Hiding %s client '%s' from user '%s' in realm '%s' - user lacks required permission",
                clientType, clientRep.getClientId(), ctx.user.getUsername(), ctx.realm.getName());
        }
        return hasPermission;
    }

    private record FilterContext(RealmModel realm, UserModel user, AccessProvider accessProvider,
                                  AccountApplicationsFilterConfig config) {}

    private AccountApplicationsFilterProvider getFilterProvider(KeycloakSession session) {
        AccountApplicationsFilterProvider provider = session.getProvider(AccountApplicationsFilterProvider.class);
        if (provider != null) {
            return provider;
        }
        return session.getProvider(AccountApplicationsFilterProvider.class, AccountApplicationsFilterFactory.PROVIDER_ID);
    }

    private AccessProvider getAccessProvider(KeycloakSession session) {
        AccessProvider defaultProvider = session.getProvider(AccessProvider.class);
        if (defaultProvider != null) {
            LOG.debugf("Using server-wide default access provider for account applications filter");
            return defaultProvider;
        }
        LOG.infof("No server-wide default access provider configured. Using '%s' as fallback for account applications filter.",
            ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
        return session.getProvider(AccessProvider.class, ClientRoleBasedAccessProviderFactory.PROVIDER_ID);
    }
}
