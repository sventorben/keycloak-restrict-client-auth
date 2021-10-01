#!/bin/bash

/opt/jboss/keycloak/bin/standalone.sh -Dkeycloak.migration.action=export -Dkeycloak.migration.provider=dir -Dkeycloak.migration.dir=/tmp/import/export -Djboss.socket.binding.port-offset=100 -Dkeycloak.migration.usersExportStrategy=REALM_FILE
