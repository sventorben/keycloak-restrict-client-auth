package de.sventorben.keycloak.authorization.client.requiredactions;

import de.sventorben.keycloak.authorization.client.common.OperationalInfo;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import java.util.Map;

public class RestrictAccessRequiredActionFactory implements RequiredActionFactory, ServerInfoAwareProviderFactory {

    static final String PROVIDER_ID = "RESTRICT_CLIENT_AUTH_DENY_ACCESS";

    @Override
    public String getDisplayText() {
        return "Restrict user authentication on clients";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession keycloakSession) {
        return new RestrictAccessRequiredAction();
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

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
}
