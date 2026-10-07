package de.sventorben.keycloak.authorization.client.clientpolicy.executor;

import de.sventorben.keycloak.authorization.client.access.AccessProvider;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRefreshResponseContext;
import org.keycloak.util.TokenUtil;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnforceAccessClientPolicyExecutorTest {

    @Mock
    KeycloakSession keycloakSession;

    @Mock
    AccessProvider accessProvider;

    @InjectMocks
    EnforceAccessClientPolicyExecutor cut;

    @BeforeEach
    void setUp() {
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
            given(keycloakSession.getProvider(AccessProvider.class, "client-role")).willReturn(accessProvider);
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

    @ParameterizedTest
    @EnumSource(value = ClientPolicyEvent.class, mode = EnumSource.Mode.EXCLUDE, names = {"TOKEN_REFRESH_RESPONSE"})
    void doNothing(ClientPolicyEvent event) {
        assertThatCode(() -> cut.executeOnEvent(() -> event)).doesNotThrowAnyException();
        verifyNoInteractions(accessProvider);
    }

}
