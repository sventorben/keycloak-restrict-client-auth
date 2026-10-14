---
layout: default
title: Introduction
nav_order: 1
---

# Restrict user authorization on clients - Introduction

This is a Keycloak extension to restrict user authorization on clients.

![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/sventorben/keycloak-restrict-client-auth?sort=semver)
![Keycloak Dependency Version](https://img.shields.io/badge/Keycloak-26.8.0-blue)
![GitHub Release Date](https://img.shields.io/github/release-date-pre/sventorben/keycloak-restrict-client-auth)
![Github Last Commit](https://img.shields.io/github/last-commit/sventorben/keycloak-restrict-client-auth)

![CI build](https://github.com/sventorben/keycloak-restrict-client-auth/actions/workflows/buildAndTest.yml/badge.svg)
![open issues](https://img.shields.io/github/issues/sventorben/keycloak-restrict-client-auth)
[![CodeScene Code Health](https://codescene.io/projects/25589/status-badges/code-health)](https://codescene.io/projects/25589)

## What is it good for?

As a Keycloak consultant, I often receive inquiries about restricting user authorization for specific clients. People commonly ask,

> Can I allow certain users to authenticate with a client while denying access to others?

While the answer used to be "no" for out-of-the-box Keycloak, I have developed this extension to meet this need. I recommend taking the time to understand the potential security implications before using this extension.
So, before using this extension, please take a moment to review the [security considerations](security.html).

## How does it work?

You restrict a client by adding a marker to it. Users may only access a restricted client if they have been granted access. All other clients are accessible to all users, unless you [deny access by default](configuration.html#denying-access-by-default).

There are two ways to restrict a client, called access providers:

| Access provider | Marker | Who has access |
|---|---|---|
| `client-role` | A client role named `restricted-access` | Users with that role, assigned directly, via groups or via composite roles |
| `policy` | An authorization resource named `Keycloak Client Resource` | Users for whom the permissions and policies of that resource evaluate to `PERMIT`. Only for confidential OpenID Connect clients with authorization enabled. |

The `auto` access provider combines both: it determines per client which restriction applies, so you can use client roles and policies in the same realm.

## Restrict clients, not flows

The extension started as an authenticator: access was checked wherever you added it to an authentication flow, using the access provider you configured for it. Every path that did not run through such a flow, e.g. single sign-on, identity provider redirects, password reset, refreshing or exchanging tokens, was not checked.

The extension now follows a different approach: **the restriction belongs to the client, and access is checked on every path.**
You restrict a client by adding the client role or the resource. You no longer need to make sure that every flow contains the authenticator and that each authenticator uses the right access provider.

| | Authenticator only | Required action and client policy executor |
|---|---|---|
| Where restrictions are defined | Per client: client role or resource | Per client: client role or resource |
| Where access is checked | Only in flows that contain the authenticator | At the end of every login, no matter which flow, and whenever tokens are issued, incl. refresh and token exchange |
| Which restrictions are checked | Only those of the configured access provider | All restrictions a client has (`auto` access provider) |
| Configuration | Per flow and per authenticator | Once per realm |

[Denying access by default](configuration.html#denying-access-by-default) takes this one step further: instead of protecting the clients you restrict, it protects all clients and lets you decide who may access the clients you have not restricted.

This is more secure, but you need to think about access differently:
* **Restricting a client restricts it everywhere.** If a client has the client role or the resource, users without access cannot get tokens for it anymore, no matter how they log in. Restrictions you only meant for one flow apply to all paths.
* **Leftovers count.** A `Keycloak Client Resource` without a permission, e.g. from an experiment, denies access to everyone. Remove restrictions you do not use.
* **Losing access takes effect immediately.** If you remove a user's role, the next token refresh fails, instead of the user keeping access until the session ends.

See the [recommended setup](configuration.html#recommended-setup) to get started.

## Quick introduction (Video interview with Niko Köbler)

The interview shows an earlier version of the extension, which only provided the authenticator.

[![Youtube Video - Interview with Niko Köbler](https://img.youtube.com/vi/eQjOCrcit24/0.jpg)](https://www.youtube.com/watch?v=eQjOCrcit24)
