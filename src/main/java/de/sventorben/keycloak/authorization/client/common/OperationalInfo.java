package de.sventorben.keycloak.authorization.client.common;

import java.util.Map;

public class OperationalInfo {

    public static Map<String, String> get() {
        String version = OperationalInfo.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "unknown";
        }
        return Map.of("Version", version);
    }
}
