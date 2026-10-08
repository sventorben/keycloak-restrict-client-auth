package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import de.sventorben.keycloak.authorization.client.access.AccessProviderResolver;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelTokenResponseContext;
import org.keycloak.protocol.oidc.grants.device.clientpolicy.context.DeviceTokenResponseContext;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AbstractClientSessionCtxTokenResponseContext;
import org.keycloak.services.clientpolicy.context.ImplicitHybridTokenResponse;
import org.keycloak.services.clientpolicy.context.JWTAuthorizationGrantResponseContext;
import org.keycloak.services.clientpolicy.context.ResourceOwnerPasswordCredentialsResponseContext;
import org.keycloak.services.clientpolicy.context.TokenExchangeResponseContext;
import org.keycloak.services.clientpolicy.context.TokenRefreshResponseContext;
import org.keycloak.services.clientpolicy.context.TokenResponseContext;
import org.keycloak.util.TokenUtil;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.BACKCHANNEL_TOKEN_RESPONSE;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.DEVICE_TOKEN_RESPONSE;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.IMPLICIT_HYBRID_TOKEN_RESPONSE;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.JWT_AUTHORIZATION_GRANT_RESPONSE;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.RESOURCE_OWNER_PASSWORD_CREDENTIALS_RESPONSE;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.TOKEN_RESPONSE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnforceAccessClientPolicyExecutorTest {

    private static final String TOKEN_RESPONSES = "de.sventorben.keycloak.authorization.client.clientpolicy.executor."
        + "EnforceAccessClientPolicyExecutorTest#tokenResponses";

    @Mock
    KeycloakSession keycloakSession;

    @Mock
    AccessProviderResolver accessProviderResolver;

    @Mock
    AccessProvider accessProvider;

    @InjectMocks
    EnforceAccessClientPolicyExecutor cut;

    @BeforeEach
    void setUp() {
        given(accessProviderResolver.resolve("client-role", "restrict-client-auth-enforce")).willReturn(accessProvider);
        cut.setupConfiguration(null);
    }

    @Nested
    class TokenRefresh {

        private static final String SESSION_ID = "session-id";

        @Mock
        KeycloakContext keycloakContext;

        @Mock
        RealmModel realm;

        @Mock
        UserSessionProvider userSessionProvider;

        @Mock
        UserSessionModel userSession;

        @Mock
        ClientModel client;

        @Mock
        UserModel user;

        @BeforeEach
        void setUp() {
            given(keycloakSession.getContext()).willReturn(keycloakContext);
            given(keycloakSession.sessions()).willReturn(userSessionProvider);
            given(keycloakContext.getClient()).willReturn(client);
            given(keycloakContext.getRealm()).willReturn(realm);
        }

        @Test
        void allowsRefreshOnUnrestrictedClient() {
            givenUserSession();
            given(accessProvider.isRestricted(client)).willReturn(false);

            assertThatCode(() -> cut.executeOnEvent(refreshContext(TokenUtil.TOKEN_TYPE_REFRESH)))
                .doesNotThrowAnyException();
        }

        @Test
        void allowsRefreshForPermittedUser() {
            givenUserSession();
            given(accessProvider.isRestricted(client)).willReturn(true);
            given(accessProvider.isPermitted(client, user)).willReturn(true);

            assertThatCode(() -> cut.executeOnEvent(refreshContext(TokenUtil.TOKEN_TYPE_REFRESH)))
                .doesNotThrowAnyException();
        }

        @Test
        void deniesRefreshForUserWithoutAccess() {
            givenUserSession();
            given(accessProvider.isRestricted(client)).willReturn(true);
            given(accessProvider.isPermitted(client, user)).willReturn(false);

            assertRefreshDenied(TokenUtil.TOKEN_TYPE_REFRESH);
        }

        @Test
        void deniesOfflineRefreshForUserWithoutAccess() {
            given(userSessionProvider.getOfflineUserSession(realm, SESSION_ID)).willReturn(userSession);
            given(userSession.getUser()).willReturn(user);
            given(accessProvider.isRestricted(client)).willReturn(true);
            given(accessProvider.isPermitted(client, user)).willReturn(false);

            assertRefreshDenied(TokenUtil.TOKEN_TYPE_OFFLINE);
        }

        @Test
        void deniesRefreshIfUserSessionDoesNotExist() {
            given(userSessionProvider.getUserSession(realm, SESSION_ID)).willReturn(null);
            given(accessProvider.isRestricted(client)).willReturn(true);
            given(client.getRealm()).willReturn(realm);

            assertRefreshDenied(TokenUtil.TOKEN_TYPE_REFRESH);
        }

        private void assertRefreshDenied(String tokenType) {
            assertThatThrownBy(() -> cut.executeOnEvent(refreshContext(tokenType)))
                .isInstanceOf(ClientPolicyException.class)
                .hasFieldOrPropertyWithValue("error", OAuthErrorException.INVALID_GRANT);
        }

        private void givenUserSession() {
            given(userSessionProvider.getUserSession(realm, SESSION_ID)).willReturn(userSession);
            given(userSession.getUser()).willReturn(user);
        }

        private TokenRefreshResponseContext refreshContext(String tokenType) {
            Map<String, String> refreshToken = Map.of("typ", tokenType, "sid", SESSION_ID);
            MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
            params.putSingle(OAuth2Constants.REFRESH_TOKEN, new JWSBuilder().jsonContent(refreshToken).none());
            return new TokenRefreshResponseContext(params, null);
        }
    }

    @Nested
    class TokenExchange {

        @Mock
        ClientSessionContext clientSessionContext;

        @Mock
        AuthenticatedClientSessionModel clientSession;

        @Mock
        UserSessionModel userSession;

        @Mock
        ClientModel requester;

        @Mock
        ClientModel audience;

        @Mock
        UserModel user;

        @BeforeEach
        void setUp() {
            given(clientSessionContext.getClientSession()).willReturn(clientSession);
            given(clientSession.getUserSession()).willReturn(userSession);
            given(userSession.getUser()).willReturn(user);
            given(clientSession.getClient()).willReturn(requester);
        }

        @Test
        void allowsExchangeOnUnrestrictedRequesterWithoutAudience() {
            givenAudience((ClientModel[]) null);
            given(accessProvider.isRestricted(requester)).willReturn(false);

            assertThatCode(() -> cut.executeOnEvent(exchangeContext())).doesNotThrowAnyException();
        }

        @Test
        void allowsExchangeForPermittedUser() {
            givenAudience(audience);
            givenAccess(requester, true);
            givenAccess(audience, true);

            assertThatCode(() -> cut.executeOnEvent(exchangeContext())).doesNotThrowAnyException();
        }

        @Test
        void deniesExchangeIfUserHasNoAccessToRequester() {
            givenAudience(audience);
            givenAccess(requester, false);

            assertExchangeDenied();
        }

        @Test
        void deniesExchangeIfUserHasNoAccessToAudience() {
            givenAudience(audience);
            givenAccess(requester, true);
            givenAccess(audience, false);

            assertExchangeDenied();
        }

        private void givenAudience(ClientModel... audienceClients) {
            given(clientSessionContext.getAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, ClientModel[].class))
                .willReturn(audienceClients);
        }

        private void givenAccess(ClientModel client, boolean permitted) {
            given(accessProvider.isRestricted(client)).willReturn(true);
            given(accessProvider.isPermitted(client, user)).willReturn(permitted);
        }

        private void assertExchangeDenied() {
            assertThatThrownBy(() -> cut.executeOnEvent(exchangeContext()))
                .isInstanceOf(ClientPolicyException.class)
                .hasFieldOrPropertyWithValue("error", OAuthErrorException.ACCESS_DENIED);
        }

        private TokenExchangeResponseContext exchangeContext() {
            return new TokenExchangeResponseContext(null, clientSessionContext, null);
        }
    }

    @Nested
    class TokenResponse {

        @Mock
        ClientSessionContext clientSessionContext;

        @Mock
        AuthenticatedClientSessionModel clientSession;

        @Mock
        UserSessionModel userSession;

        @Mock
        ClientModel client;

        @Mock
        UserModel user;

        @BeforeEach
        void setUp() {
            given(clientSession.getClient()).willReturn(client);
            given(clientSession.getUserSession()).willReturn(userSession);
            given(userSession.getUser()).willReturn(user);
        }

        @ParameterizedTest
        @MethodSource(TOKEN_RESPONSES)
        void allowsTokenOnUnrestrictedClient(ClientPolicyEvent event,
                                             Class<? extends ClientPolicyContext> contextClass) {
            given(accessProvider.isRestricted(client)).willReturn(false);

            assertThatCode(() -> cut.executeOnEvent(context(event, contextClass))).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @MethodSource(TOKEN_RESPONSES)
        void allowsTokenForPermittedUser(ClientPolicyEvent event, Class<? extends ClientPolicyContext> contextClass) {
            given(accessProvider.isRestricted(client)).willReturn(true);
            given(accessProvider.isPermitted(client, user)).willReturn(true);

            assertThatCode(() -> cut.executeOnEvent(context(event, contextClass))).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @MethodSource(TOKEN_RESPONSES)
        void deniesTokenForUserWithoutAccess(ClientPolicyEvent event, Class<? extends ClientPolicyContext> contextClass,
                                             String expectedError) {
            given(accessProvider.isRestricted(client)).willReturn(true);
            given(accessProvider.isPermitted(client, user)).willReturn(false);

            assertThatThrownBy(() -> cut.executeOnEvent(context(event, contextClass)))
                .isInstanceOf(ClientPolicyException.class)
                .hasFieldOrPropertyWithValue("error", expectedError);
        }

        private ClientPolicyContext context(ClientPolicyEvent event,
                                            Class<? extends ClientPolicyContext> contextClass) {
            ClientPolicyContext context = mock(contextClass);
            given(context.getEvent()).willReturn(event);
            if (context instanceof DeviceTokenResponseContext) {
                given(((DeviceTokenResponseContext) context).getClientSession()).willReturn(clientSession);
            } else {
                given(((AbstractClientSessionCtxTokenResponseContext) context).getClientSessionContext())
                    .willReturn(clientSessionContext);
                given(clientSessionContext.getClientSession()).willReturn(clientSession);
            }
            return context;
        }
    }

    static Stream<Arguments> tokenResponses() {
        return Stream.of(
            arguments(TOKEN_RESPONSE, TokenResponseContext.class, OAuthErrorException.INVALID_GRANT),
            arguments(RESOURCE_OWNER_PASSWORD_CREDENTIALS_RESPONSE,
                ResourceOwnerPasswordCredentialsResponseContext.class, OAuthErrorException.INVALID_GRANT),
            arguments(JWT_AUTHORIZATION_GRANT_RESPONSE, JWTAuthorizationGrantResponseContext.class,
                OAuthErrorException.INVALID_GRANT),
            arguments(DEVICE_TOKEN_RESPONSE, DeviceTokenResponseContext.class, OAuthErrorException.ACCESS_DENIED),
            arguments(BACKCHANNEL_TOKEN_RESPONSE, BackchannelTokenResponseContext.class,
                OAuthErrorException.ACCESS_DENIED),
            arguments(IMPLICIT_HYBRID_TOKEN_RESPONSE, ImplicitHybridTokenResponse.class,
                OAuthErrorException.ACCESS_DENIED)
        );
    }

    @ParameterizedTest
    @EnumSource(value = ClientPolicyEvent.class, mode = EnumSource.Mode.EXCLUDE,
        names = {"TOKEN_REFRESH_RESPONSE", "TOKEN_EXCHANGE_RESPONSE", "TOKEN_RESPONSE",
            "RESOURCE_OWNER_PASSWORD_CREDENTIALS_RESPONSE", "JWT_AUTHORIZATION_GRANT_RESPONSE", "DEVICE_TOKEN_RESPONSE",
            "BACKCHANNEL_TOKEN_RESPONSE", "IMPLICIT_HYBRID_TOKEN_RESPONSE"})
    void doNothing(ClientPolicyEvent event) {
        assertThatCode(() -> cut.executeOnEvent(() -> event)).doesNotThrowAnyException();
        verifyNoInteractions(accessProvider);
    }

}
