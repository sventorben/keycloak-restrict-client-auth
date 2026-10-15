package de.sventorben.keycloak.authorization.client.uma;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import de.sventorben.keycloak.authorization.client.access.auto.AutoAccessProviderFactory;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.authorization.AuthorizationTokenService;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.grants.PermissionGrantType;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;
import org.keycloak.services.CorsErrorResponseException;

import java.util.Optional;

/**
 * Denies requesting party tokens (UMA grant) for users who do not have access to the restricted resource server.
 * Keycloak does not trigger client policies for this grant, so the client policy executor does not cover it.
 * Determines the user and the resource server the same way as Keycloak, before Keycloak evaluates any permission.
 * Service accounts are not checked.
 */
final class RestrictedPermissionGrantType extends PermissionGrantType {

    static final String PROVIDER_ID = "restrict-client-auth-uma";

    private static final String ACCESS_DENIED_MESSAGE = "Access to client is denied.";

    @Override
    public Response process(Context context) {
        setContext(context);
        UserModel user = getUser();
        ClientModel resourceServer = getResourceServer();
        if (user != null && resourceServer != null && !isPermitted(resourceServer, user)) {
            event.user(user).detail(Details.AUDIENCE, resourceServer.getClientId()).error(Errors.ACCESS_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, ACCESS_DENIED_MESSAGE,
                Response.Status.FORBIDDEN);
        }
        return super.process(context);
    }

    private boolean isPermitted(ClientModel resourceServer, UserModel user) {
        AccessProvider accessProvider = new AccessProviderResolver(session)
            .resolve(AutoAccessProviderFactory.PROVIDER_ID, PROVIDER_ID);
        return !accessProvider.isRestricted(resourceServer) || accessProvider.isPermitted(resourceServer, user);
    }

    private UserModel getUser() {
        return Optional.ofNullable(getIdentityToken())
            .map(JsonWebToken::getSubject)
            .map(subject -> session.users().getUserById(realm, subject))
            .filter(user -> user.getServiceAccountClientLink() == null)
            .orElse(null);
    }

    /**
     * The token representing the identity asking for permissions: the bearer token, a claim token in ID token
     * format, or a subject token. Keycloak verifies it afterwards and rejects the request if it is invalid.
     */
    private JsonWebToken getIdentityToken() {
        String authorizationHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer")) {
            return Tokens.getAccessToken(session);
        }
        String claimToken = formParams.getFirst("claim_token");
        String claimTokenFormat = Optional.ofNullable(formParams.getFirst("claim_token_format"))
            .orElse(AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN);
        String token = claimToken != null && AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN.equalsIgnoreCase(
            claimTokenFormat) ? claimToken : formParams.getFirst("subject_token");
        return token == null ? null : session.tokens().decode(token, JsonWebToken.class);
    }

    /**
     * The resource server identified by the permission ticket, or by the audience if no ticket is given.
     */
    private ClientModel getResourceServer() {
        String ticket = formParams.getFirst("ticket");
        String clientId = ticket == null
            ? formParams.getFirst("audience")
            : Optional.ofNullable(session.tokens().decode(ticket, PermissionTicketToken.class))
                .map(PermissionTicketToken::getIssuedFor)
                .orElse(null);
        return clientId == null ? null : realm.getClientByClientId(clientId);
    }
}
