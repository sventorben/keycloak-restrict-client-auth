* [deps] Update Keycloak dependencies to 18.0.2
* [feat] Add support for [client policies](https://www.keycloak.org/docs/latest/server_admin/#_client_policies).
  * Provide a [client policy condition](https://www.keycloak.org/docs/latest/server_admin/#condition) named `restrict-client-auth-enabled` to check whether user authentication on a client has been restricted or not.
  * Provide a [client policy executor](https://www.keycfloak.org/docs/latest/server_admin/#executor) named `restrict-client-auth-auto-config` to automatically enable restricted access for clients.

  > ⚠️ **Feature preview**:
  >
  > Support for client policies is currently feature preview. I am happy to get some feedback on this.
  > However, depending on feedback the feature may be changed  or even be removed again in the future.
