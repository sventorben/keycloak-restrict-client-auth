---
layout: default
title: Recipes
nav_order: 6
---

# Recipes
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Grant access to several clients via groups

Suppose you have three clients and teams that need access to different combinations of them.

1) Add a client role named `restricted-access` to every client.
2) Create a group per team.
3) Open a group, go to `Role mapping`, click `Assign role`, filter by clients, and assign the `restricted-access` roles of the clients the team may access.

Each client has its own `restricted-access` role, so a group can grant access to any combination of clients. Users get access via their groups.

## Grant access via a client role or policies

Suppose users may access a confidential client if they have a certain role, or if they meet other conditions, e.g. group membership or user attributes.

1) Restrict the client via [policies](configuration.html#resource-policy-based).
2) Add a `Role` policy for the role and further policies for the other conditions.
3) Add all policies to the permission of the `Keycloak Client Resource` and set its decision strategy to `Affirmative`.

Access is granted if any of the policies permits it.

{: .warning }
> Do not add two authenticators with different access providers as `Alternative` steps to a flow to achieve this. Keycloak lets users choose between alternatives, e.g. via "Try another way", so users who are denied by one restriction can pick the other one.

## Deny access to users with a certain role

Suppose all users may access a client except those with a certain role, e.g. external users.

For confidential clients, restrict the client via [policies](configuration.html#resource-policy-based) and add a `Role` policy for that role with `Logic` set to `Negative` to the permission. Access is granted to all users without the role, on all paths.

Alternatively, use Keycloak's built-in authenticators in a flow: a conditional sub-flow with `Condition - User Role` and `Deny Access`. Keep in mind that this only applies to the flows you add it to, see [other flows](authenticator.html#other-flows).

## Use client roles for public clients and policies for confidential clients

Keycloak does not allow authorization services for public clients, e.g. single-page or mobile apps. Restrict public clients via [client roles](configuration.html#client-role-based) and confidential clients via [policies](configuration.html#resource-policy-based). The required action and the `restrict-client-auth-enforce` client policy executor check both, since they use the [`auto` access provider](configuration.html#using-client-roles-and-policies-in-the-same-realm). If you also use the authenticator, select `auto` as its access provider.

## Introduce a new user pool

Suppose a realm has existing users who may access all clients, and you add users, e.g. of a partner, who may only access some clients.

1) Create a realm role, e.g. `default-access`, and assign it to the existing users via a group.
2) Configure it as the default access role and enable [Deny access by default](configuration.html#denying-access-by-default).
3) Restrict the clients the new users may access and grant access to the new users. Grant access to the existing users as well, since the default access role only applies to clients that are not restricted.

Existing users keep access to all clients that are not restricted. New users may only access the clients they have been granted access to, even if you forget to restrict a client.
