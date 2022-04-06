# Keycloak: Restrict user authorization on clients

This is a simple Keycloak authenticator to restrict user authorization on clients.

![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/sventorben/keycloak-restrict-client-auth?sort=semver)
![Keycloak Dependency Version](https://img.shields.io/badge/Keycloak-17.0.1-blue)
![Keycloak.X ready](https://img.shields.io/badge/%E2%AD%90%20Keycloak.X%20%E2%AD%90-ready%20-blue)
![GitHub Release Date](https://img.shields.io/github/release-date-pre/sventorben/keycloak-restrict-client-auth)
![Github Last Commit](https://img.shields.io/github/last-commit/sventorben/keycloak-restrict-client-auth)

![CI build](https://github.com/sventorben/keycloak-restrict-client-auth/actions/workflows/buildAndTest.yml/badge.svg)
![open issues](https://img.shields.io/github/issues/sventorben/keycloak-restrict-client-auth)

> ⚠️ **Deprecation warning**:
>
> With the release of Keycloak 17 the Quarkus-based distribution is now fully supported by the Keycloak team.
> I will therefore deprecate support for the Wildfly-based distro from now on.
>
> You will find deprecation marks (👎) of deprecated functionality within the readme below.
>
> I will fully drop support once the Keycloak teams drops support for the Wildfly-based distro.

## What is it good for?

Every now and then I get asked whether it is possible to restrict user authorization on certain clients.

Generally the question goes like this

> Is it possible to allow specific users to authenticate to a client while rejecting others?

As of today, my general answer is _"no, at least not out of the box"_. And in general my advice is to redesign and relocate the policy enforcement point (PEP).
However, since this question popped up so often, I wrote this simple authenticator to support this functionality.

## How does it work?

The authenticator can work either role-based or policy-based.

### Role-based mode

In this mode, the authenticator uses client roles to restrict authentication. It works like this:

* The authenticator checks whether a client defines a role named `restricted-access`
    * If it does the authenticator checks whether the user has that role
        * If it does, the authenticator returns success (i.e. authentication is successful)
        * If it does not, the authenticator returns failure (i.e. authentication is unsuccessful)
    * If it does not, the authenticator returns success (i.e. authentication is successful).

This means that you can enable the authenticator on a per-client basis by adding a client role named `restricted-access` to your client.
A client with that role has the authenticator enabled. Only users with that role can authenticate to that client.

### Policy-based mode

In this mode, the authenticator uses client resources, permissions and policies to restrict authentication.
This mode only works for confidential OIDC clients with authorization enabled.
It works like this:

* The authenticator checks whether a client defines a resource named `Keycloak Client Resource`
    * If it does, the authenticator checks whether policies and permission evaluate to `PERMIT`
        * If it does, the authenticator returns success (i.e. authentication is successful)
        * If it does not, the authenticator returns failure (i.e. authentication is unsuccessful)
    * If it does not, the authenticator returns success (i.e. authentication is successful).

This means that you can enable the authenticator on a per-client basis by adding a resource named `Keycloak Client Resource` to your client.
A client with that resource has the authenticator enabled. Users will only be able to authenticate to such a client if the associated policies and permission permit access.

## How to install?

Download a release (*.jar file) that works with your Keycloak version from the [list of releases](https://github.com/sventorben/keycloak-restrict-client-auth/releases).
Follow the below instructions depending on your distribution and runtime environment.

### Quarkus-based distro (Keycloak.X)

Copy the jar to the `providers` folder and execute the following command:

```shell
${kc.home.dir}/bin/kc.sh build
```

### Container image (Docker)

For Docker-based setups mount or copy the jar to
- `/opt/jboss/keycloak/providers` for Wildfly-based distro or Keycloak.X prior to version `15.1.0` (👎)
- `/opt/keycloak/providers` for Keycloak.X from version `15.1.0`

You may want to check [docker-compose.yml](docker-compose.yml) as an example.

### Wildfly-based distro 👎

Create a Wildfly module and deploy it to your Keycloak instance. For details please refer to the [official documentation](https://www.keycloak.org/docs/latest/server_development/#register-a-provider-using-modules).

For convenience, here is a `module.xml` file.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.3" name="keycloak-restrict-client-auth">
    <resources>
        <resource-root path="keycloak-restrict-client-auth.jar"/>
    </resources>
    <dependencies>
        <module name="org.keycloak.keycloak-services"/>
    </dependencies>
</module>
```

### Maven/Gradle

Packages are being released to GitHub Packages. You find the coordinates [here](https://github.com/sventorben/keycloak-restrict-client-auth/packages/779937/versions)!

It may happen that I remove older packages without prior notice, because the storage is limited on the free tier.


## How to configure?

* Add the authenticator to the desired flow.
* Mark the authenticator as `Required`.
* Then bind your newly created flow as desired - either as a default for the whole realm or on a per-client basis.

  See the image below for an example.
![Example flow](docs/images/flow.jpg)

* Follow instructions below for the desired mode

> ⚠️ **User identity**:
>
> The authenticator needs a user identity to check whether the user has the desired role or not. Hence, ensure that you have steps/executions in your flow prior to this authenticator that can ensure user's identity.

### Role-based mode

1) Configure the authenticator by clicking on `Actions -> Config` and select `client-role` as the `Access Provider`.
![Role-based access restriction](docs/images/config-client-role-based.jpg)
2) Add a role named `restricted-access` to the client you want to restrict access to.

   See the image below for an example.
![Client role configuration](docs/images/client-role.jpg)
3) Afterwards, no user can authenticate to this client. To allow a user to authenticate, assign the role `restricted-access` to the user. You may do so either by assigning the role to the user directly or via groups or combined roles.

#### Changing the role name

You do not like the role name `restricted-access` or you do have some kind of naming conventions in place? You can change the role name globally by configuring the provider.

##### Quarkus-based distro (Keycloak.X):
```properties
spi-restrict-client-auth-access-provider-client-role-enabled=true
spi-restrict-client-auth-access-provider-client-role-client-role-name=custom-role
```

For details on SPI and provider configuration in Keycloak.X, please refer to [Configuring providers](https://www.keycloak.org/server/configuration-provider) guide.

##### Wildfly-based distro (👎)

###### via CLI:
```
/subsystem=keycloak-server/spi=restrict-client-auth-access-provider:add(default-provider=client-role)
/subsystem=keycloak-server/spi=restrict-client-auth-access-provider/provider=client-role:add(properties={clientRoleName=my-custom-role-name,enabled=true})
```

###### via standalone.xml:
```XML
<spi name="restrict-client-auth-access-provider">
    <default-provider>client-role</default-provider>
    <provider name="client-role" enabled="true">
        <properties>
            <property name="clientRoleName" value="my-custom-role-name"/>
        </properties>
    </provider>
</spi>
```

For details, please refer to [Manage Subsystem Configuration](https://www.keycloak.org/docs/latest/server_installation/index.html#manage-subsystem-configuration) section in the server installation guide.


### Policy-based mode

> ⚠️ **OIDC only**:
>
> Policy-based mode only works with OIDC clients (`Client Protocol` must be `openid-connect`)


1) Configure the authenticator by clicking on `Actions -> Config` and select `policy` as the `Access Provider`.
![Policy-based access restriction](docs/images/config-policy-based.jpg)
2) Configure the `Access Type` of the client to `confidential`
3) Set `Authorization Enabled` to `on`
4) Go to `Authorization -> Resources` and click `Create` to create a new resource
   ![Resource configuration](docs/images/resource-config.jpg)
5) Set the `Name` and `Display name` to `Keycloak Client Resource` and keep the other fields blank
6) Save the resource
   ![Resources overview](docs/images/resource.jpg)
7) Click `Create Permission` to add permissions and policies (see [Authorization Services Guide](https://www.keycloak.org/docs/latest/authorization_services/#_permission_overview) for details)
8) Afterwards, no user can authenticate to this client unless permissions have been granted by configured policies.

### Using a custom error message

If a user tries to log in via a browser-based flow and access gets denied by the authenticator, a custom error message can be displayed.
In the flow choose the `Actions` button and then choose `Config`. You will see the following configuration screen.

![Error message configuration](docs/images/config-message.jpg)

You can directly define a particular message or use a property, which will be used for mapping the error message. If you choose a property, the property will be looked up from your custom theme's `messages*.properties` files and therefore supports internationalization.

```properties
# messages.properties
restricted-access.denied=Access denied. User is missing required role 'restricted-access'
# messages_de.properties
restricted-access.denied=Zugriff verweigert. Dem Benutzer fehlt die notwendige Rolle 'restricted-access'.
```

If the field is left blank, default property `access-denied` is used. In this case you do not need a custom theme, since this property comes with Keycloak out of the box.
For details on how to add custom messages to Keycloak, please refer to section [Messages and Internationalization](https://www.keycloak.org/docs/latest/server_development/#messages) in the server developer guide.

## Frequently asked questions

### Does it (already) work with Keycloak.X?
On October 28th 2021 the Keycloak project [announced](https://www.keycloak.org/2021/10/keycloak-x-update) the roadmap for the new Quarkus-based Keycloak-X distribution.
According to this Keycloak 16 will be the last preview of the Quarkus distribution. As of December 2021, Keycloak 17 will make the Quarkus distribution fully supported the WildFly distribution will be deprecated.
Support for the Wildfly distribution will be removed by mid 2022.

Meanwhile Keycloak 17 has been released and is fully based on Quarkus. Hence, the Wildfly-based distro has been deprecated.

Therefore, I will focus all further development of this library towards the Quarkus-based Keycloak.X distribution.
Once the Wildfly support will be removed from the Keycloak project, I will remove all support for Wildfly here as well.

Don't worry, I will ensure this library stays compatible with the Wildfly distribution as well as with Keycloak.X until then. However, I will start to deprecate certain information in the README and no longer accept Wildfly-related feature requests.

### Does it work with Keycloak version X.Y.Z?

If you are using Keycloak version `X` (e.g. `X.y.z`), version `X.b.c` should be compatible.
Keycloak SPIs are quite stable. So, there is a high chance this authenticator will work with other versions, too. Check the details of latest [build results](https://github.com/sventorben/keycloak-restrict-client-auth/actions/workflows/buildAndTest.yml) for an overview or simply give it a try.

Authenticator version `X.b.c` is compiled against Keycloak version `X.y.z`. For example, version `16.3.1` will be compiled against Keycloak version `16.y.z`.

I do not guarantee what version `a.b` or `y.z` will be. Neither do I backport features to older version, nor maintain any older versions of this authenticator. If you need the latest features or bugfixes for an older version, please fork this project or update your Keycloak instance. I recommend doing the latter on regular basis anyways.

### Why not use "Allow/Deny Access" authenticators with conditions?

With Keycloak 13 two new authenticators have been added, namely `Allow Access` and `Deny Access`. Together with `Condition - User Role` authenticator authentication may be restricted in a similar way with out-of-the-box features. So, the question is why not use that and override authentication flows on a per client basis?

Here are some reasons/thoughts
* It is not really flexible. Since `Condition - User Role` only allows for checking one concrete (realm or client-specific) role, a very complex flow handling all clients, or a totally separate flow for each individual client would be needed.
* It simply does not work well with federated authentication (ie. identity provider redirects), since there is no way to configure client specific behaviour for `First login flow` or `Post login flows`. In other words, there is no feature like `Authentication flow overrides` at an IdP level. Hence, the same flow will be used for all clients. As said before, this becomes very complicated.
