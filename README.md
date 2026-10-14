# Keycloak: Restrict user authorization on clients

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

## How does it work?

You restrict a client by adding a client role named `restricted-access` or an authorization resource named `Keycloak Client Resource` to it. Users may only access a restricted client if they have the role or if the policies of the resource permit access.

The restriction belongs to the client, and access is checked on every path: a required action checks access at the end of every login, no matter which authentication flow has been used, and a client policy executor checks access whenever tokens are issued, including refresh and token exchange.

## Documentation

Please refer to the [documentation website](https://sventorben.github.io/keycloak-restrict-client-auth/) for instructions on installation, configuration, features, and general usage.

[![Youtube Video - Interview with Niko Köbler](https://img.youtube.com/vi/eQjOCrcit24/0.jpg)](https://www.youtube.com/watch?v=eQjOCrcit24)
