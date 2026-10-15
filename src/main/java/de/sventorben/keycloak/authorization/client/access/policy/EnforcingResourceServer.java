package de.sventorben.keycloak.authorization.client.access.policy;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

/**
 * Evaluates permissions as if the resource server enforced its policies, no matter which policy enforcement mode is
 * configured. Otherwise, Keycloak permits all requests in mode DISABLED, and requests to resources without permissions
 * in mode PERMISSIVE, i.e. all users would have access to the restricted client.
 */
final class EnforcingResourceServer implements ResourceServer {

    private final ResourceServer delegate;

    EnforcingResourceServer(ResourceServer delegate) {
        this.delegate = delegate;
    }

    @Override
    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return PolicyEnforcementMode.ENFORCING;
    }

    @Override
    public void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getClientId() {
        return delegate.getClientId();
    }

    @Override
    public boolean isAllowRemoteResourceManagement() {
        return delegate.isAllowRemoteResourceManagement();
    }

    @Override
    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return delegate.getDecisionStrategy();
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        throw new UnsupportedOperationException();
    }
}
