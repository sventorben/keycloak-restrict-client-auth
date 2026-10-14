---
layout: default
title: Notice for Developers
nav_order: 9
---

# Notice for Developers
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Custom access providers

Access providers decide which clients are restricted and which users may access them. Besides the built-in access providers `client-role` and `policy`, you can add your own via the `restrict-client-auth-access-provider` SPI.

1. Implement `de.sventorben.keycloak.authorization.client.access.AccessProvider` and `AccessProviderFactory`.
2. Register your factory in `META-INF/services/de.sventorben.keycloak.authorization.client.access.AccessProviderFactory`.
3. Deploy your jar next to the jar of this extension.

Your access provider can then be selected in the authenticator and in the `restrict-client-auth-auto-config` client policy executor. The `auto` access provider, which the required action and the `restrict-client-auth-enforce` client policy executor always use, considers it as well.

### Contract

| Method | Contract |
|---|---|
| `isRestricted(client)` | Whether this access provider restricts access to the given client, e.g. because the client has a certain client role or resource. |
| `isPermitted(client, user)` | Whether the given user may access the given client. Callers only ask for clients this access provider restricts; access to other clients is decided by the caller, e.g. via [denying access by default](configuration.html#denying-access-by-default). For clients this access provider does not restrict, deny access, so that access is never granted by mistake, e.g. if the restriction has been removed meanwhile. |
| `enableFor(client)` | Restricts access to the given client, e.g. by adding a client role or resource. Used by the `restrict-client-auth-auto-config` client policy executor. |

{: .warning }
> The SPI is marked as internal. Its interfaces may change in future releases. Check the release notes when upgrading.
