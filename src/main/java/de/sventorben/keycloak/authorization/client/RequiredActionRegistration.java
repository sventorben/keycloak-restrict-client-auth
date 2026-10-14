package de.sventorben.keycloak.authorization.client;

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;

/**
 * Registers and enables the required action in all realms by default: in existing realms on startup, and in new realms
 * when they are created or imported. Realms in which the required action is already registered, no matter whether
 * enabled or disabled, are left untouched.
 */
final class RequiredActionRegistration {

    private static final Logger LOG = Logger.getLogger(RequiredActionRegistration.class);

    private static final String TASK_KEY = "restrict-client-auth::register-required-action";
    private static final int TASK_TIMEOUT_IN_SECONDS = 60;

    private RequiredActionRegistration() {
    }

    static void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent) {
            KeycloakModelUtils.runJobInTransaction(((PostMigrationEvent) event).getFactory(),
                RequiredActionRegistration::registerInAllRealms);
        } else if (event instanceof RealmModel.RealmPostCreateEvent) {
            registerIfAbsent(((RealmModel.RealmPostCreateEvent) event).getCreatedRealm());
        }
    }

    private static void registerInAllRealms(KeycloakSession session) {
        // only one node of a cluster registers the required action on startup
        session.getProvider(ClusterProvider.class).executeIfNotExecuted(TASK_KEY, TASK_TIMEOUT_IN_SECONDS, () -> {
            session.realms().getRealmsStream().forEach(RequiredActionRegistration::registerIfAbsent);
            return null;
        });
    }

    static void registerIfAbsent(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias(RestrictClientAuthRequiredActionFactory.PROVIDER_ID) != null) {
            return;
        }
        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(RestrictClientAuthRequiredActionFactory.PROVIDER_ID);
        requiredAction.setProviderId(RestrictClientAuthRequiredActionFactory.PROVIDER_ID);
        requiredAction.setName(RestrictClientAuthRequiredActionFactory.DISPLAY_TEXT);
        requiredAction.setEnabled(true);
        requiredAction.setDefaultAction(false);
        // run after all other required actions, e.g. after updating the password
        requiredAction.setPriority(realm.getRequiredActionProvidersStream()
            .mapToInt(RequiredActionProviderModel::getPriority)
            .max()
            .orElse(0) + 1);
        realm.addRequiredActionProvider(requiredAction);
        LOG.infof("Registered and enabled required action '%s' in realm '%s'.",
            RestrictClientAuthRequiredActionFactory.PROVIDER_ID, realm.getName());
    }
}
