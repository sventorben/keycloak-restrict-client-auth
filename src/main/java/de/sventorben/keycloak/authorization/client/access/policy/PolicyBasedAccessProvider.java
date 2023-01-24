package de.sventorben.keycloak.authorization.client.access.policy;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.admin.PolicyEvaluationService;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.common.UserModelIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;

import java.util.Collections;

import static java.util.Collections.emptyList;

public final class PolicyBasedAccessProvider implements AccessProvider {

    private static final Logger LOG = Logger.getLogger(PolicyBasedAccessProvider.class);
    private static final String RESOURCE_NAME = "Keycloak Client Resource";

    private final KeycloakSession keycloakSession;

    PolicyBasedAccessProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public boolean isRestricted(ClientModel client) {
        return getResource(client) != null;
    }

    @Override
    public boolean isPermitted(ClientModel client, UserModel user) {
        AuthorizationProvider authorization = keycloakSession.getProvider(AuthorizationProvider.class);
        Resource resource = getResource(client, authorization);
        ResourceServer resourceServer = getResourceServer(client, authorization);

        if (resource == null || resourceServer == null) {
            LOG.warnf("Possible configuration issue: Could not find resource '%s' for client '%s' in realm '%s'.",
                RESOURCE_NAME, client.getClientId(), client.getRealm().getName());
            return false;
        }

        ResourcePermission resourcePermission = new ResourcePermission(resource, emptyList(), resourceServer);
        Identity identity = new UserModelIdentity(client.getRealm(), user);
        DefaultEvaluationContext evaluationContext = new DefaultEvaluationContext(identity, keycloakSession);
        PolicyEvaluationService.EvaluationDecisionCollector decision = authorization.evaluators()
            .from(Collections.singleton(resourcePermission), evaluationContext)
            .evaluate(new PolicyEvaluationService.EvaluationDecisionCollector(authorization, resourceServer,
                new AuthorizationRequest()));

        if (decision == null || decision.getResults().isEmpty()) {
            logAccessDenied(client, user);
            LOG.warnf(
                "Possible configuration issue: Did you forget to add a permission or policy for client '%s' and resource '%s' in realm '%s'",
                client.getClientId(), RESOURCE_NAME, client.getRealm().getName());
            return false;
        }

        boolean permitted = decision.getResults().stream()
            .allMatch(evaluationResult -> Decision.Effect.PERMIT.equals(evaluationResult.getEffect()));
        if (!permitted) {
            logAccessDenied(client, user);
        } else {
            LOG.debugf(
                "Access for user '%s' to resource '%s' on client '%s' in realm '%s' granted.",
                user.getUsername(), RESOURCE_NAME, client.getId(), client.getRealm().getName());
        }
        return permitted;

    }

    @Override
    public void enableFor(ClientModel client) {
        if (isRestricted(client)) return;

        client.setPublicClient(false);
        client.setBearerOnly(false);

        AuthorizationProvider authorization = keycloakSession.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client);
        storeFactory.getResourceStore().create(resourceServer, RESOURCE_NAME, resourceServer.getClientId());
    }

    @Override
    public void close() {
    }

    private void logAccessDenied(ClientModel client, UserModel user) {
        LOG.warnf(
            "Access for user '%s' is denied. User does not have permission to access resource '%s' on client '%s' in realm '%s'.",
            user.getUsername(), RESOURCE_NAME, client.getId(), client.getRealm().getName());
    }

    private ResourceServer getResourceServer(ClientModel client, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        return storeFactory.getResourceServerStore().findByClient(client);
    }

    private Resource getResource(ClientModel client) {
        AuthorizationProvider authorization = keycloakSession.getProvider(AuthorizationProvider.class);
        return getResource(client, authorization);
    }

    private Resource getResource(ClientModel client, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client);
        if (resourceServer == null) {
            return null;
        }
        return storeFactory.getResourceStore().findByName(resourceServer, RESOURCE_NAME);
    }
}
