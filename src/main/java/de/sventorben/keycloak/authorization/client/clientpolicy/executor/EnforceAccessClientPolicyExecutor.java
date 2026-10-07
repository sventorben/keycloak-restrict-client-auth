package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRefreshResponseContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.util.TokenUtil;

import java.util.Objects;

class EnforceAccessClientPolicyExecutor implements ClientPolicyExecutorProvider<EnforceAccessClientPolicyExecutorConfiguration> {

    private static final Logger LOG = Logger.getLogger(EnforceAccessClientPolicyExecutor.class);

    static final String PROVIDER_ID = "restrict-client-auth-enforce";

    private final KeycloakSession keycloakSession;
    private EnforceAccessClientPolicyExecutorConfiguration configuration;

    EnforceAccessClientPolicyExecutor(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public Class<EnforceAccessClientPolicyExecutorConfiguration> getExecutorConfigurationClass() {
        return EnforceAccessClientPolicyExecutorConfiguration.class;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case TOKEN_REFRESH_RESPONSE:
                enforce(keycloakSession.getContext().getClient(), getUser((TokenRefreshResponseContext) context));
                break;
        }
    }

    private void enforce(ClientModel client, UserModel user) throws ClientPolicyException {
        final AccessProvider access = new AccessProviderResolver(keycloakSession)
            .resolve(configuration.getAccessProviderId(), PROVIDER_ID);

        if (!access.isRestricted(client)) {
            return;
        }

        if (user == null) {
            LOG.warnf("Could not determine user to check access to client '%s' in realm '%s'. Access is denied.",
                client.getClientId(), client.getRealm().getName());
        } else if (access.isPermitted(client, user)) {
            return;
        }

        throw new ClientPolicyException(OAuthErrorException.INVALID_GRANT, "Access to client is denied.");
    }

    private UserModel getUser(TokenRefreshResponseContext context) {
        // Keycloak has already verified the refresh token at this point. Look up the user session the same way
        // Keycloak does when validating the refresh token, instead of relying on claims that mappers may change.
        RefreshToken refreshToken;
        try {
            refreshToken = new JWSInput(context.getParams().getFirst(OAuth2Constants.REFRESH_TOKEN))
                .readJsonContent(RefreshToken.class);
        } catch (JWSInputException e) {
            LOG.warn("Could not read refresh token.", e);
            return null;
        }

        RealmModel realm = keycloakSession.getContext().getRealm();
        UserSessionModel userSession;
        if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(refreshToken.getType())) {
            userSession = keycloakSession.sessions().getOfflineUserSession(realm, refreshToken.getSessionId());
        } else {
            userSession = keycloakSession.sessions().getUserSession(realm, refreshToken.getSessionId());
        }
        return userSession == null ? null : userSession.getUser();
    }

    @Override
    public void setupConfiguration(EnforceAccessClientPolicyExecutorConfiguration config) {
        configuration = Objects.requireNonNullElseGet(config,
            EnforceAccessClientPolicyExecutorConfiguration::new).parseWithDefaultValues();
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
}
