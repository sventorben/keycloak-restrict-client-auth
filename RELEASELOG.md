* [deps] Update Keycloak dependencies to 17.0.0
* [compatibility] Dropping compatibility support for Keycloak versions before 15.1.1

  This means I will no longer check if this extension is compatible with versions before 15.1.1. Though any future versions may be compatible,  I will no longer ensure such compatibility.
* [deprecation] Deprecating support for Wildfly-based Keycloak distro

  This means that from now on new features may not be compatible with the Wildfly-based distro. However, I will try to keep this extension compatible with the Wildfly-based Keycloak distro until the Keycloak team fully drops support.
