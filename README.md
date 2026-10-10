# Distrimarket shared library and local stack

Compras, Inventario, Ventas, and the gateway all consume
`com.distrimarket:distrimarket-common:1.0.1-SNAPSHOT`. Entities, compatibility DTOs,
generated DTOs, the gateway OpenAPI contract, and Liquibase migrations live here.
Generate DTOs from `src/main/resources/static/openapi.yaml`; changes to generated
files under `target/` are discarded on the next build.

Keep these six repositories in the same parent directory:
`distrimarket-common`, `distrimarket-eureka`, `distrimarket-gateway`,
`distrimarket-ms-featureA`, `distrimarket-ms-featureB`, and `distrimarket-ms-featureC`.

## Build

Use Java 21 and Maven 3.9 or later. From this repository:

```sh
python3 scripts/build_stack.py
```

The script installs common first, then tests and packages all five applications.
Set `MAVEN=/path/to/mvn` when Maven is not on PATH. To isolate dependencies,
pass `--repository /tmp/distrimarket-m2`; dependencies must be available there or
downloadable from Maven Central. Maven returns a failure if any repository fails.

## Shared database and discovery

Create a development PostgreSQL database and export the same configuration in
each service terminal. Example:

```sh
export DB_HOST=localhost DB_PORT=5432 DB_NAME=pos_db DB_USER=postgres
export DB_PASSWORD=local-password DB_SSLMODE=disable
export EUREKA_URL=http://localhost:8761/eureka/
export LIQUIBASE_ENABLED=true
```

For hosted PostgreSQL use `DB_SSLMODE=require` (the default).
`SPRING_DATASOURCE_URL` overrides `DB_URL`, which overrides the individual
connection variables. The old `DB_SSL_MODE` spelling also works.
All services use the shared Liquibase history and lock; Hibernate does not update
the schema. Keep existing changelogs immutable and append new migrations.

Run each JAR from its repository in a separate terminal, starting Eureka first:

| Repository | Command | Default port |
| --- | --- | --- |
| distrimarket-eureka | `java -jar target/distrimarket-eureka-1.0.0-SNAPSHOT.jar` | 8761 |
| distrimarket-ms-featureB | `java -jar target/distrimarket-inventario-1.0.0.jar` | 8081 |
| distrimarket-ms-featureC | `java -jar target/distrimarket-ms-featureC-0.0.1-SNAPSHOT.jar` | 8082 |
| distrimarket-ms-featureA | `java -jar target/distrimarket-ms-featureA-1.0-SNAPSHOT.jar` | 8083 |
| distrimarket-gateway | `java -jar target/distrimarket-gateway-1.0.0-SNAPSHOT.jar` | 8080 |

Wait for Eureka registration before calling the gateway. Application ports can
be overridden with `SERVER_PORT`; the gateway and microservices also accept `PORT`.

## Gateway APIs

Open <http://localhost:8080/swagger-ui.html>. The default contract uses the gateway
origin and includes every service. The gateway preserves service path prefixes:

| Service | Example through the gateway | Service OpenAPI through the gateway |
| --- | --- | --- |
| Inventario | `/inventario/productos` | `/inventario/v3/api-docs` |
| Compras | `/compras/facturas` | `/compras/v3/api-docs` |
| Ventas | `/ventas/facturas-venta` | `/ventas/v3/api-docs` |

`/ventas/facturas` is an alias for `/ventas/facturas-venta`.
`/medios-pago` and its child paths route to `/ventas/medios-pago`.
Compras retains `persona.idPersona` and `deposito.idDeposito` in nested responses;
Inventario and Ventas retain their canonical `id` fields. Pagination is preserved:
Compras and payment methods use `contenido`/`numeroPagina`/`tamanio`, Inventario
uses Spring Page fields, and the remaining Ventas lists use `content`/`pageNumber`/`pageSize`.

## Integration verification

After building, run:

```sh
python3 scripts/verify_stack.py
```

Requires Python 3, Java 21, and PostgreSQL binaries (`initdb`, `pg_ctl`, `psql`) on
PATH. Run as a regular user, since `initdb` cannot run as root. It starts a new
PostgreSQL cluster and all applications on available local ports, checks discovery,
all controller groups, Swagger, validation, and compatibility, then verifies stock
changes `0 → 10 → 7 → 10` through purchase, sale, and cancellation. A full stack
restart must preserve stock and Liquibase history. Processes are stopped on exit;
the temporary directory printed at startup retains database files and logs.
Inherited database and discovery settings are replaced for this disposable run.

When executable JAR packaging dependencies are unavailable but application
dependencies are cached, use compiled classes:

```sh
python3 scripts/build_stack.py --classes --offline --repository /tmp/distrimarket-m2
python3 scripts/verify_stack.py --classes
```

This fallback tests compiled applications with Maven's test classpath, including
provided and test dependencies. Verify normal executable JAR packaging once Maven
Central is accessible.
