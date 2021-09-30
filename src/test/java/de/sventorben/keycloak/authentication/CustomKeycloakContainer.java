package de.sventorben.keycloak.authentication;

import dasniko.testcontainers.keycloak.KeycloakContainer;

final class CustomKeycloakContainer extends KeycloakContainer {

    private String providerClassLocation;

    CustomKeycloakContainer(String dockerImageName) {
        super(dockerImageName);
    }

    CustomKeycloakContainer withProviderClassesFrom(String classesLocation) {
        this.providerClassLocation = classesLocation;
        return this;
    }

    @Override
    protected void configure() {
        super.configure();
        if (providerClassLocation != null) {
            createKeycloakExtensionDeployment("/opt/jboss/keycloak/providers", "providers.jar", providerClassLocation);
        }
    }
}
