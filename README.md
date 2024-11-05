# pagoPA Receipt-pdf-datastore

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-gpd-ingestion-manager&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-gpd-ingestion-manager)

Java Azure Functions that ingest a message from GPD observer event hub, tokenizes sensitive information and sends it
again to the event hub

---

## Summary 📖

- [Api Documentation 📖](#api-documentation-)
- [Start Project Locally 🚀](#start-project-locally-)
    * [Run locally with Docker](#run-locally-with-docker)
        + [Prerequisites](#prerequisites)
        + [Run docker container](#run-docker-container)
    * [Run locally with Maven](#run-locally-with-maven)
        + [Prerequisites](#prerequisites-1)
        + [Set environment variables](#set-environment-variables)
        + [Run the project](#run-the-project)
    * [Test](#test)
- [Develop Locally 💻](#develop-locally-)
    * [Prerequisites](#prerequisites-2)
    * [Testing 🧪](#testing-)
        + [Unit testing](#unit-testing)
        + [Integration testing](#integration-testing)
        + [Performance testing](#performance-testing)
- [Contributors 👥](#contributors-)
    * [Maintainers](#maintainers)

---

## Start Project Locally 🚀

### Run locally with Docker

#### Prerequisites

- docker

#### Set environment variables

`docker build -t pagopa-gpd-ingestion-manager .`

`cp .env.example .env`

and replace in `.env` with correct values

#### Run docker container

then type :

`docker run -p 80:80 --env-file=./.env pagopa-gpd-ingestion-manager`

### Run locally with Maven

#### Prerequisites

- maven

#### Set environment variables

On terminal type:

`cp local.settings.json.example local.settings.json`

then replace env variables with correct values
(if there is NO default value, the variable HAS to be defined)

| VARIABLE                                       | USAGE                                                                             |                     DEFAULT VALUE                      |
|------------------------------------------------|-----------------------------------------------------------------------------------|:------------------------------------------------------:|
| `PAYMENT_POSITION_INPUT_EVENTHUB_CONN_STRING`  | Connection string to the PaymentPosition input topic                              |                                                        |
| `PAYMENT_POSITION_OUTPUT_EVENTHUB_CONN_STRING` | Connection string to the PaymentPosition output topic                             |                                                        |
| `PDV_TOKENIZER_BASE_PATH`                      | PDV Tokenizer API base path                                                       | "https://api.uat.tokenizer.pdv.pagopa.it/tokenizer/v1" |
| `PDV_TOKENIZER_CREATE_TOKEN_ENDPOINT`          | PDV Tokenizer API create token endpoint                                           |                       "/tokens"                        |
| `PDV_TOKENIZER_SUBSCRIPTION_KEY`               | API azure ocp apim subscription key                                               |                                                        |
| `PDV_TOKENIZER_INITIAL_INTERVAL`               | PDV Tokenizer initial interval for retry a request that fail with 429 status code |                          200                           |
| `PDV_TOKENIZER_MULTIPLIER`                     | PDV Tokenizer interval multiplier for subsequent request retry                    |                          2.0                           |
| `PDV_TOKENIZER_RANDOMIZATION_FACTOR`           | PDV Tokenizer randomization factor for interval retry calculation                 |                          0.6                           |
| `PDV_TOKENIZER_MAX_RETRIES`                    | PDV Tokenizer max request retry                                                   |                           3                            |
| `TOKENIZER_APIM_HEADER_KEY`                    | Tokenizer APIM header key                                                         |                       x-api-key                        |

> to doc details about AZ fn config
>
see [here](https://stackoverflow.com/questions/62669672/azure-functions-what-is-the-purpose-of-having-host-json-and-local-settings-jso)

#### Run the project

`mvn clean package`

`mvn azure-functions:run`

### Test

`curl http://localhost:8080/info`

---

## Develop Locally 💻

### Prerequisites

- git
- maven
- jdk-11

### Testing 🧪

#### Unit testing

To run the **Junit** tests:

`mvn clean verify`

#### Integration testing

#### Performance testing

---

## Contributors 👥

Made with ❤️ by PagoPa S.p.A.

### Maintainers

See `CODEOWNERS` file