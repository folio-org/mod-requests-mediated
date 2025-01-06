# mod-requests-mediated

Copyright (C) 2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Goal

FOLIO compatible mediated requests functionality, including secure requests.

### Environment variables

| Name                 | Default value             | Description                                                                                                                                                                           |
|:---------------------|:--------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JAVA_OPTIONS         | -XX:MaxRAMPercentage=66.0 | Java options                                                                                                                                                                          |
| DB_HOST              | postgres                  | Postgres hostname                                                                                                                                                                     |
| DB_PORT              | 5432                      | Postgres port                                                                                                                                                                         |
| DB_USERNAME          | folio_admin               | Postgres username                                                                                                                                                                     |
| DB_PASSWORD          | folio_admin               | Postgres username password                                                                                                                                                            |
| DB_DATABASE          | okapi_modules             | Postgres database name                                                                                                                                                                |
| KAFKA_HOST           | kafka                     | Kafka broker hostname                                                                                                                                                                 |
| KAFKA_PORT           | 9092                      | Kafka broker port                                                                                                                                                                     |
| SYSTEM_USER_USERNAME | mod-requests-mediated     | Username for `mod-requests-mediated` system user                                                                                                                                      |
| SYSTEM_USER_PASSWORD | mod-requests-mediated     | Password for `mod-requests-mediated` system user (not required for dev envs)                                                                                                          |
| SYSTEM_USER_ENABLED  | true                      | Defines if system user must be created at service tenant initialization                                                                                                               |
| SECURE_TENANT_ID     | -                         | Defines name of the tenant secure tenant                                                                                                                                              |
| OKAPI_URL            | -                         | OKAPI URL used to login system user, required                                                                                                                                         |
| ENV                  | folio                     | The logical name of the deployment, must be unique across all environments using the same shared Kafka/Elasticsearch clusters, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed |


## Further information

### Issue tracker

Project [MODREQMED](https://issues.folio.org/browse/MODREQMED).

