## :boom: Breaking changes

**The required action `Restrict user authentication on clients` is enabled by default.** It is registered and enabled in all realms on startup, and in new realms when they are created or imported. Realms in which it is already registered, enabled or disabled, are left untouched.

This changes the behavior of realms with restricted clients:
- Users without access are now also denied if they log in via a path that bypassed the authenticator, e.g. single sign-on, identity provider redirects, password reset, registration, or links sent via email.
- The required action uses the `auto` access provider, so it also enforces restrictions via policies. A leftover `Keycloak Client Resource` without a permission denies access to everyone. Remove restrictions you do not use.
- For the resource owner password credentials grant on restricted clients without the authenticator in the direct grant flow, Keycloak responds with `Account is not fully set up` for users without access.

To keep the previous behavior in a realm, disable the required action under Authentication → Required actions. Do not remove it, since it would be registered and enabled again on the next start.

If you are locked out of the admin realm, disable the required action provider via the server configuration (`spi-required-action--restrict-client-auth-enforce--enabled=false`, or `spi-required-action-restrict-client-auth-enforce-enabled=false` on Keycloak 26.2; a build time option), log in, fix the configuration, and remove the option again. Alternatively, use a temporary admin service account. See ["Locked out?"](https://sventorben.github.io/keycloak-restrict-client-auth/faq.html#locked-out) in the documentation.

**The authenticator responds with standard OAuth errors.** For requests that do not accept HTML, e.g. the resource owner password credentials grant, it responds with errors according to RFC 6749: `invalid_grant` with status 400 from the token endpoint, and `access_denied` with status 403 otherwise. So far, it responded with `access-denied` and status 401, which is not an OAuth error code. Update clients that check for the previous error.

## Restrict clients, not flows

This release changes how the extension protects your clients: **the restriction belongs to the client, and access is checked on every path.**

So far, access was only checked where the authenticator was part of an authentication flow, using the access provider configured for it. Paths without such a flow were not checked, e.g. single sign-on, identity provider redirects, password reset, registration, links sent via email, refreshing or exchanging tokens.

Now you restrict a client by adding the client role or the resource, as before, and two new enforcement points check access on all other paths:

| Enforcement point | Checks access |
| --- | --- |
| Required action `Restrict user authentication on clients` | At the end of every login, no matter which authentication flow has been used. Runs after other required actions, so users can reset their password before access is denied (#121). |
| Client policy executor `restrict-client-auth-enforce` | Whenever tokens are issued: authorization code, refresh incl. offline tokens (#425), token exchange (Keycloak 26.8+), resource owner password credentials, device, CIBA, JWT authorization grant, implicit and hybrid flow. |

**Known limitation:** requesting party tokens issued via the UMA grant are not checked yet. Users without access to a restricted client can obtain them if it is a resource server whose permissions grant them other resources. See ["Requesting party tokens"](https://sventorben.github.io/keycloak-restrict-client-auth/configuration.html#requesting-party-tokens) for a workaround.

See ["Restrict clients, not flows"](https://sventorben.github.io/keycloak-restrict-client-auth/) and the ["Recommended setup"](https://sventorben.github.io/keycloak-restrict-client-auth/configuration.html#recommended-setup) in the documentation for details.

## New features

- **`auto` access provider**: determines per client which restriction applies, i.e. the client role or the resource, and considers all installed access providers, including custom ones. Use client roles and policies in the same realm with a single configuration, e.g. client roles for public clients and policies for confidential clients. The required action and the `restrict-client-auth-enforce` executor always use `auto`. The authenticator's default access provider remains `client-role`.
- **Deny access by default**: realm settings in the configuration of the required action. If enabled, all clients are restricted; access to clients without a restriction is only permitted for users with the configured default access role. The settings apply even if the required action is disabled and can be managed via the admin console, the admin REST API, Terraform (`keycloak_required_action`), and realm export and import.
- **Error message after password update**: the required action can show a different error message if users updated their password during the same login, e.g. after resetting it (`passwordUpdatedErrorMessage`).
- **Audience check**: the `restrict-client-auth-enforce` executor can optionally check access to all clients in the audience (`aud`) of access tokens (`checkAudience`).
- **Service account check**: the `restrict-client-auth-enforce` executor can optionally check whether service accounts have access to the restricted clients in the audience of their tokens (`checkServiceAccounts`). Service accounts are granted access like users, e.g. via the client role in the `Service account roles` tab. In the admin realm, this also applies to the temporary admin service account recommended for lockouts.

## Documentation website

The documentation moved from the README to a [website](https://sventorben.github.io/keycloak-restrict-client-auth/), with new pages on the authenticator and other flows, error messages, and recipes, and with screenshots of the current admin console.

## Upgrade notes

- Authenticators without a configured access provider now use the server-wide default access provider, if one is configured via `spi-restrict-client-auth-access-provider--provider-default` (#550). So far, they always used `client-role`.
- Restrictions via policies are now evaluated as with policy enforcement mode `Enforcing`, regardless of the mode configured for the client (#556). If you use `Permissive` or `Disabled`, check that your permissions grant access as intended.
- If the configured access provider does not exist, e.g. after a typo in a realm import or after removing a custom access provider, access to all clients is denied (#557). So far, the server-wide default access provider or `client-role` was used. This also applies to the required action and the `restrict-client-auth-enforce` executor if the server is configured with `spi-restrict-client-auth-access-provider--provider=<id>` instead of `provider-default`.
- Denied access is logged at debug level instead of as a warning (#558). Keycloak records it as `LOGIN_ERROR` event with error `access_denied`. Warnings about configuration issues remain.
- Apart from the changes above, existing setups behave as before. All other new enforcement points and options are disabled by default. The authenticator works as before.
- The `restrict-client-auth-auto-config` executor no longer sets `publicClient` and `bearerOnly` to `false` when restricting access via the `policy` access provider, and no longer fails for clients without authorization enabled (#539). Creating clients no longer fails with HTTP 500 while the executor is active (#555).
- The README previously claimed that tokens containing a client the user has no access to in their audience are not issued. This only holds with the new `checkAudience` option.
