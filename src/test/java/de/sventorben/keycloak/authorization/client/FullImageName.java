package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import static de.sventorben.keycloak.authorization.client.FullImageName.Distribution.quarkus;
import static de.sventorben.keycloak.authorization.client.FullImageName.Distribution.wildfly;
import static java.lang.module.ModuleDescriptor.*;

class FullImageName {

    enum Distribution {
        quarkus,
        wildfly
    }

    private static final Distribution KEYCLOAK_DIST = Distribution.valueOf(System.getProperty("keycloak.dist", quarkus.name()));

    private static final String LATEST_VERSION = "latest";
    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", LATEST_VERSION);

    static String get() {
        String imageName = "keycloak";
        String imageVersion = KEYCLOAK_VERSION;

        if (LATEST_VERSION.equalsIgnoreCase(KEYCLOAK_VERSION)) {
            if (wildfly.equals(KEYCLOAK_DIST)) {
                imageVersion = "17.0.1-legacy";
            }
        } else {
            if (getParsedVersion().compareTo(Version.parse("17")) >= 0) {
                if (wildfly.equals(KEYCLOAK_DIST)) {
                    imageVersion = KEYCLOAK_VERSION + "-legacy";
                }
            } else {
                if (quarkus.equals(KEYCLOAK_DIST)) {
                    imageName = "keycloak-x";
                }
            }
        }

        return "quay.io/keycloak/" + imageName + ":" + imageVersion;
    }

    static Boolean isLatestVersion() {
        return LATEST_VERSION.equalsIgnoreCase(KEYCLOAK_VERSION);
    }

    static Version getParsedVersion() {
        if (isLatestVersion()) {
            return null;
        }
        return Version.parse(KEYCLOAK_VERSION);
    }

    static Distribution getDistribution() {
        return KEYCLOAK_DIST;
    }

    static KeycloakContainer createContainer() {

        String fullImage = FullImageName.get();

        if (quarkus.equals(KEYCLOAK_DIST) &&
            !LATEST_VERSION.equalsIgnoreCase(KEYCLOAK_VERSION) && getParsedVersion().compareTo(Version.parse("15.1")) < 0) {
            return new KeycloakXContainer(fullImage);
        }
        return new KeycloakContainer(fullImage);

    }

}
