package de.sventorben.keycloak.authorization.client.requiredactions;

import de.sventorben.keycloak.authorization.client.RestrictClientAuthConfig;
import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.resetcred.AbstractSetRequiredActionAuthenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.MediaTypeMatcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

public final class RestrictClientAuthRequiredActionAuthenticator extends AbstractSetRequiredActionAuthenticator implements ServerInfoAwareProviderFactory {

    private static final Logger LOG = Logger.getLogger(RestrictClientAuthRequiredActionAuthenticator.class);

    private static final String PROVIDER_ID = "restrict-client-auth-action-auth";

    public RestrictClientAuthRequiredActionAuthenticator() {}

    @Override
    public void authenticate(final AuthenticationFlowContext context) {

        if (context.getExecution().isRequired()) {

            final ClientModel client = context.getSession().getContext().getClient();
            final RestrictClientAuthConfig config = new RestrictClientAuthConfig(context.getAuthenticatorConfig());

            final AccessProvider access = new AccessProviderResolver(config).resolve(context);

            final UserModel user = context.getUser();
            if (access.isRestricted(client) && !access.isPermitted(client, user)) {
                context.getAuthenticationSession().addRequiredAction(RestrictAccessRequiredActionFactory.PROVIDER_ID);
            }
        }

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        LOG.warn("Action called!");
        context.failure(AuthenticationFlowError.ACCESS_DENIED);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return OperationalInfo.get();
    }

    @Override
    public String getDisplayType() {
        return "Restrict user authentication on clients (via required action)";
    }

    @Override
    public String getHelpText() {
        return "Restricts user authentication on clients based on an access provider. Should be used in reset credentials flow. Access will be denied during required action evaluation.";
    }
}
