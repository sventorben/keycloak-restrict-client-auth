---
layout: default
title: Security considerations
nav_order: 7
---

# Security considerations
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Policy enforcement

To avoid any confusion, it is important to note that this extension is not a policy enforcement point (PEP).
It does not enforce authorization decisions. It is part of making the authorization decision, but it does not enforce it.
Clients must enforce decisions being made.

According to the [OIDC specification/OpenID Connect Core 1.0 incorporating errata set 1](https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation), it is the responsibility of the client to check the audience claims. Section 3.1.3.7. ID Token validation, point 3ff state that
> The Client MUST validate that the `aud` (audience) Claim contains its `client_id` value registered at the Issuer identified by the `iss` (issuer) Claim as an audience. [...]

And further it is very clear about the enforcement by the client:
> The ID Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it contains additional audiences not trusted by the Client.
> If the ID Token contains multiple audiences, the Client SHOULD verify that an `azp` Claim is present.
> If an `azp` (authorized party) Claim is present, the Client SHOULD verify that its client_id is the Claim Value.

So, by design this extension cannot be a PEP.

What this extension supports you with is authorization decision-making and communicating the outcome to the user during login flow. If configured correctly, it will prevent issuance of tokens that contain an authorized party claim for a client that users do not have access to. To also prevent issuance of tokens that contain such a client in their audience, enable the `Check audience` option of the `restrict-client-auth-enforce` [client policy executor](configuration.html#checking-the-audience).

Token issuance or non-issuance is (beside claims in the token) just one way to communicate the outcome of authorization decision-making. It is not enforcement of the decision.

Make sure your clients enforce decisions correctly and ensure validation of `aud` (audience) and `azp` (authorized party) claims.

If you are using a Keycloak adapter, make sure your clients are verifying the audience claim by enabling `verify-token-audience` in your [adapter config](https://www.keycloak.org/docs/latest/securing_apps/#_java_adapter_config).

## Protect all possible flows

The [recommended setup](configuration.html#recommended-setup) checks access on all paths. If you only use the authenticator, ensure that you protect authentication to your clients in all flows a user may access. This includes not just the browser flow or the other realm-wide flows, but also identity provider overrides and post login flows.

Here is one example: suppose a user tries to log in via the built-in browser flow, at the end of which you have added the "Restrict user authentication on clients" step. If the "Cookie" or "Forms" alternative is used, the user will proceed to this step and be evaluated. But if it is the "Identity Provider Redirector" alternative which gets used, the subsequent steps will be skipped and the user will not be subject to this validation (this is a general feature of how brokering works in Keycloak authentication flows, not specific to this extension). This extension must also be configured in the identity provider's post login flow in order to apply.

To check access no matter which authentication flow has been used, keep the [required action](configuration.html#required-action) enabled. See [other flows](authenticator.html#other-flows) for which enforcement point covers which path.

Refreshing or exchanging tokens does not run any authentication flow at all. If users should lose access when their roles or permissions change, configure the `restrict-client-auth-enforce` [client policy executor](configuration.html#checking-access-whenever-tokens-are-issued). For OpenID Connect clients, the executor also denies issuing tokens if the authenticator has been skipped during login.

## Permissions to change access

Keycloak's [fine-grained admin permissions](https://www.keycloak.org/docs/latest/server_admin/#_fine_grained_permissions) control who may change which part of the access control:

| What | Required permission |
|---|---|
| Change the [realm settings](configuration.html#realm-settings) | Realm role `manage-realm` of the `realm-management` client. Fine-grained admin permissions do not cover realm settings. |
| Restrict a client or remove the restriction, i.e. create or delete the client role or the resource | `manage` on the client |
| Grant a user access to a restricted client via the client role | `map-roles` on the user and `map-role` on the client role |
| Grant access via a group | `manage-membership` on the group |
| Grant the default access role, i.e. access to all clients that are not restricted | `map-role` on the default access role. Restrict this permission to as few administrators as possible. |

Without [denying access by default](configuration.html#denying-access-by-default), administrators who may manage a client can remove its restriction and thereby grant access to all users. If access is denied by default, removing the restriction only grants access to users with the default access role.

## Disable the `Audience Resolve` mapper if necessary

The [`Audience Resolve` protocol mapper](https://www.keycloak.org/docs/latest/server_admin/#_audience_resolve) is enabled by default by client scope `roles`, but it may be necessary to remove it in some cases.
Failing to set up audience claims correctly may result in a token containing the restricted client as an audience claim, even if the user does not have access to that client. Alternatively, enable the `Check audience` option of the `restrict-client-auth-enforce` [client policy executor](configuration.html#checking-the-audience).
