# mod-requests-mediated

Copyright (C) 2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Goal

FOLIO compatible mediated requests functionality, including secure requests.

### Environment variables

| Name                                     | Default value             | Description                                                                                                                                                                           |
|:-----------------------------------------|:--------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JAVA_OPTIONS                             | -XX:MaxRAMPercentage=66.0 | Java options                                                                                                                                                                          |
| DB_HOST                                  | postgres                  | Postgres hostname                                                                                                                                                                     |
| DB_PORT                                  | 5432                      | Postgres port                                                                                                                                                                         |
| DB_USERNAME                              | folio_admin               | Postgres username                                                                                                                                                                     |
| DB_PASSWORD                              | folio_admin               | Postgres username password                                                                                                                                                            |
| DB_DATABASE                              | okapi_modules             | Postgres database name                                                                                                                                                                |
| KAFKA_HOST                               | kafka                     | Kafka broker hostname                                                                                                                                                                 |
| KAFKA_PORT                               | 9092                      | Kafka broker port                                                                                                                                                                     |
| KAFKA_SECURITY_PROTOCOL                  | PLAINTEXT                 | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                                           |
| KAFKA_SSL_KEYSTORE_LOCATION              | -                         | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                                                          |
| KAFKA_SSL_KEYSTORE_PASSWORD              | -                         | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                                                |
| KAFKA_SSL_TRUSTSTORE_LOCATION            | -                         | The location of the Kafka trust store file.                                                                                                                                           |
| KAFKA_SSL_TRUSTSTORE_PASSWORD            | -                         | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled.                            |
| SYSTEM_USER_USERNAME                     | mod-requests-mediated     | Username for `mod-requests-mediated` system user                                                                                                                                      |
| SYSTEM_USER_PASSWORD                     | mod-requests-mediated     | Password for `mod-requests-mediated` system user (not required for dev envs)                                                                                                          |
| SYSTEM_USER_ENABLED                      | true                      | Defines if system user must be created at service tenant initialization                                                                                                               |
| SECURE_TENANT_ID                         | -                         | Defines name of the tenant secure tenant                                                                                                                                              |
| OKAPI_URL                                | http://okapi:9130         | OKAPI URL used to login system user, required                                                                                                                                         |
| ENV                                      | folio                     | The logical name of the deployment, must be unique across all environments using the same shared Kafka/Elasticsearch clusters, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed |
| BATCH_REQUEST_THREADS_NUM                | 4                         | Defines the number of threads for Fork-Join Pool used by flow engine for processing mediated batch requests execution.                                                                |
| BATCH_REQUEST_EXECUTION_TIMEOUT          | 10m                       | Maximum execution timeout for batch requests processing.                                                                                                                              |
| BATCH_REQUEST_LAST_EXECUTIONS_CACHE_SIZE | 25                        | Max cache size for the latest flow executions of batch requests processing.                                                                                                           |
| BATCH_REQUEST_PRINT_RESULTS              | false                     | Defines if flow engine should print batch requests execution results in logs or not                                                                                                   |
| BATCH_REQUEST_MAX_ITEMS                  | 50                        | Defines the maximum number of items allowed in a single multi-item batch request.                                                                                                     |


## Multi-Item Batch Requests

### Configuration for the validation of maximum allowed items in a single multi-item batch request

Module provides a way to control the maximum number of items allowed in a single multi-item batch request through global setting or
if the setting is not found through environment variable `BATCH_REQUEST_MAX_ITEMS` (default value is 50).

To configure the maximum number of items allowed in a single multi-item batch request through global setting, we need to add the below configuration in mod-settings:

**Permissions**
To make a post call to mod-settings, user should have below permissions.
```
  mod-settings.entries.item.post
  mod-settings.global.write.mod-requests-mediated.manage
```

**Example request**
```
POST https://{okapi-url}/settings/entries
{
  "id": "65de6432-be11-48ba-9686-a65101634040",
  "scope": "mod-requests-mediated.manage",
  "key": "multiItemBatchRequestItemsValidation",
  "value": {
    "maxAllowedItemsCount": 100
  }
}
```

## Further information

### Issue tracker

Project [MODREQMED](https://issues.folio.org/browse/MODREQMED).
