* Update Keycloak dependencies from version 15.0.0 to 15.0.2
* Support policy-based access restrictions to leverage Keycloak's resources, permissions, and policies.
  This only works on confidential OIDC clients with authorization enabled.
* Support adding additional mechanisms to enable and evaluate client access restrictions by a custom SPI.
  The SPI is marked internal for now and may change or even be removed anytime without prior notice.

> ⚠️ **Configuration changes**:
>
> * The way to configure a custom client role name has changed. It needs to be configured via the newly introduced custom SPI.
> * The way to install the authenticator has changed. It needs to be deployed as a provider/module now instead as a deployment.
>
> Please check [README.md](README.md) for details.
>
