package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import java.util.ArrayList;
import java.util.List;

final class KeycloakXContainer extends KeycloakContainer {

    private String providerClassLocation;

    KeycloakXContainer(String fullImage) {
        super(fullImage);
    }

    @Override
    protected void configure() {
        super.configure();
        if (providerClassLocation != null) {
            createKeycloakExtensionDeployment("/opt/jboss/keycloak/providers", "providers.jar", providerClassLocation);
        }
        List<String> commandParts = new ArrayList<>();
        commandParts.add("--auto-config");
        commandParts.add("--http-enabled=true");
        this.setCommand(commandParts.toArray(new String[0]));
    }

    @Override
    public KeycloakContainer withProviderClassesFrom(String classesLocation) {
        this.providerClassLocation = classesLocation;
        return super.withProviderClassesFrom(classesLocation);
    }
}
