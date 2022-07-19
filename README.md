# CZERTAINLY EJBCA NG Connector

> This repository is part of the commercial open-source project CZERTAINLY, but the connector is available under subscription. You can find more information about the project at [CZERTAINLY](https://github.com/3KeyCompany/CZERTAINLY) repository, including the contribution guide.

EJBCA NG `Connector` is the implementation of the following `Function Groups` and `Kinds`:

| Function Group | Kind |
| --- | --- |
| `Authority Provider` | `EJBCA` |
| `Discovery Provider` | `EJBCA`, `EJBCA_SCHEDULE` |

EJBCA NG `Connector` is the implementation of certificate management for EJBCA that is compatible with the v2 client operations interafce. The `Connector` is developed to work with SOAP Web Services calls.

> It is expected that the REST API calls will be implemented as option in the future release, because of some limitations of the EJBCA Web Service, for example limiting the number of end entities and certificates, that can be fetched.

EJBCA NG `Connector` allows you to perform the following operations:

`Authority Provider`
- Issue certificate
- Renew certificate
- Revoke certificate

`Discovery Provider`
- Discover certificates

## Database requirements

EJBCA NG `Connector` requires the PostgreSQL database to store the data.

## Short Process Description

EJBCA NG works under the principle of `RA Profiles`. The `Connector` provides the pathway for communication with the instances of EJBCA Certification Authorities. Multiple `Authorities` can be added using the same `Connector`. Once the `Authorities` are added, `RA Profiles` will be created on top of the `Authorities`.

With the help of `RA Profiles` and the CSR information provided by the `Client` using the REST API, the `Connector` communicates with the `Authority` to get the `Certificate`.

To know more about the `Core`, refer to [CZERTAINLY Core](https://github.com/3KeyCompany/CZERTAINLY-Core)

## Certificate Discovery

The `Certificate` discovery in the EJBCA NG `Connector` works with the V2 `Certificate` Search API from EJBCA. Older versions of EJBCA that do not support V2 Search API are not supported.

There are two types of `Discovery`:
- `EJBCA`
- `EJBCA_SCHEDULE`

### `RA Profile` attributes

The attributes for creating a new `RA Profile` includes:
- End Entity Profile Name
- Certificate Profile Name
- Certificate Authority Name
- Key Recovery enabled/disabled
- Send Notifications enabled/disabled
- Username generation method
- Username generation prefix
- Username generation postfix

### Issue `Certificate` attributes

For issuing of new `Certificate`, you can use the following optional attributes for the End Entity:
- Email address
- Subject Alternative Names
- Extension data

The EJBCA username and attributes for to issue `Certificate` are written as `Metadata` in the `Certificate` object and can be used in future operations.

### Discover `Certificate` attributes

For discovering `Certificates` from the EJBCA, the following attributes can be used:
- Authority Instance Name
- API Base URL
- Certificate Authority
- Certificate Profile
- End Entity Profile
- Date after which the certificates were issued

## Interfaces

EJBCA NG `Connector` implements `v2 Authority Provider` and `Discovery Provider` interfaces. To learn more about the interfaces and end points, refer to the [CZERTAINLY Interfaces](https://github.com/3KeyCompany/CZERTAINLY-Interfaces).

For more information, please refer to the [CZERTAINLY documentation](https://docs.czertainly.com).

## Docker container

EJBCA NG `Connector` is provided as a Docker container. Use the `docker pull harbor.3key.company/czertainly/czertainly-ejbca-ng-connector:tagname` to pull the required image from the repository. It can be configured using the following environment variables:

| Variable        | Description                       | Required | Default value |
|-----------------|-----------------------------------|----------|---------------|
| `JDBC_URL`      | JDBC URL for database access      | Yes      | N/A           |
| `JDBC_USERNAME` | Username to access the database   | Yes      | N/A           |
| `JDBC_PASSWORD` | Password to access the database   | Yes      | N/A           |
| `DB_SCHEMA`     | Database schema to use            | No       | ejbca         |
| `PORT`          | Port where the service is exposed | No       | 8082          |
