# Distrimarket shared library and local stack

Compras, Inventario, Ventas, Auth, and the gateway all consume
`com.distrimarket:distrimarket-common:1.0.1-SNAPSHOT`. Entities, compatibility DTOs,
generated DTOs, the gateway OpenAPI contract, and Liquibase migrations live here.
Generate DTOs from `src/main/resources/static/openapi.yaml`; changes to generated
files under `target/` are discarded on the next build.

Keep these six repositories in the same parent directory:
`distrimarket-common`, `distrimarket-gateway`, `distrimarket-ms-featureA`,
`distrimarket-ms-featureB`, `distrimarket-ms-featureC`, and `distrimarket-ms-auth`.

## Build

Use Java 21 and Maven 3.9 or later. From this repository:

```sh
python3 scripts/build_stack.py
```

The script installs common first, then tests and packages the five applications
(gateway, Compras, Inventario, Ventas, Auth). Set `MAVEN=/path/to/mvn` when Maven
is not on PATH. To isolate dependencies, pass `--repository /tmp/distrimarket-m2`;
dependencies must be available there or downloadable from Maven Central. Maven
returns a failure if any repository fails.

## Shared database and infrastructure

The database is Supabase (or another hosted PostgreSQL). The compose file in this
repository starts only Redis:

```sh
docker compose -f infra/docker-compose.yml up -d
```

It also supports `--profile insight` for Redis Insight. Copy `.env.example` to
`.env.local` and set `DB_URL` (preferred) or the individual `DB_*` variables for
your Supabase project. Use `DB_SSLMODE=require` for hosted PostgreSQL.
`SPRING_DATASOURCE_URL` overrides `DB_URL`, which overrides the individual
connection variables. The old `DB_SSL_MODE` spelling also works.
All services use the shared Liquibase history and lock; Hibernate does not update
the schema. Keep existing changelogs immutable and append new migrations.

Auth uses Redis for caching. If Redis is unavailable, start it with the compose
file or set `CACHE_ENABLED=false`; the service falls back to the database.
The integration verifier starts its own disposable PostgreSQL cluster; it never
touches the Supabase database.

## Cómo ejecutar

No hay Eureka: el gateway usa rutas estáticas y los servicios pueden arrancar en
cualquier orden.

```sh
cd distrimarket-common
docker compose -f infra/docker-compose.yml up -d   # Redis (opcional)
export MAVEN=/ruta/a/mvn                          # si mvn no está en PATH
python3 scripts/build_stack.py --classes
python3 scripts/run_stack.py
```

`run_stack.py` requiere el build con `--classes` (genera `target/runtime-classpath.txt`).

| Servicio | Puerto |
| --- | --- |
| Gateway | 8080 |
| Inventario | 8081 |
| Ventas | 8082 |
| Compras | 8083 |
| Auth | 8084 |

Swagger: <http://localhost:8080/swagger-ui.html> · Auth: <http://localhost:8084/auth/swagger-ui.html>

Login: `POST /auth/login` con `admin` / `Admin123!`; enviar `Authorization: Bearer <token>`.
Compras exige rol `ADMIN` o `COMPRADOR`.

La base es Supabase (`.env.local`); la primera ejecución aplica la migración 005.
Sin Redis usar `CACHE_ENABLED=false`.

Detener:

```sh
for d in distrimarket-ms-featureB distrimarket-ms-featureC \
         distrimarket-ms-featureA distrimarket-ms-auth distrimarket-gateway; do
  [ -f "$d/target/local-run.pid" ] && kill "$(cat "$d/target/local-run.pid")" 2>/dev/null || true
done
docker compose -f infra/docker-compose.yml down
```

Verificar sin tocar Supabase: `python3 scripts/verify_stack.py` (`--cache` requiere Redis).

## Security

Auth issues HS256 tokens from `POST /auth/login`. The same `JWT_SECRET` and
`JWT_ISSUER` must be configured in Auth and in every service that validates
tokens. `JWT_SECRET` must be at least 32 characters.

Compras is the first service enforcing tokens:

| Service | Allowed roles |
| --- | --- |
| Compras | `ADMIN`, `COMPRADOR` |
| Inventario | currently open (next step) |
| Ventas | `ADMIN`, `VENDEDOR` |
| Auth `/usuarios`, `/roles` | `ADMIN` |

Send the token as `Authorization: Bearer <token>`. Documentation, Swagger,
health/info, and `/error` stay public so the gateway Swagger UI keeps working.
The shared implementation lives in `com.distrimarket.commons.security` and is
enabled per service with `security.enabled: true`.

## Caché (Compras)

Compras cachea en Redis, por ID, proveedores (30m), facturas de compra (10m) y
órdenes de compra (10m). Las claves usan `distrimarket:compras:<dominio>:<id>`.
Los listados no se cachean completos: se guarda cada elemento por su ID.

```sh
docker compose -f infra/docker-compose.yml up -d   # Redis
```

`CACHE_ENABLED=false` desactiva la caché sin afectar las lecturas. La verificación
de integración la desactiva por defecto; con `--cache` usa un Redis que debe estar
escuchando en `127.0.0.1:6379`. Si Redis no responde, Compras sigue funcionando
contra la base y registra el fallo en el log.

## Gateway APIs

Open <http://localhost:8080/swagger-ui.html>. The combined contract includes every
service, including Auth. The gateway preserves service path prefixes:

| Service | Example through the gateway | Service OpenAPI through the gateway |
| --- | --- | --- |
| Inventario | `/inventario/productos` | `/inventario/v3/api-docs` |
| Compras | `/compras/facturas` | `/compras/v3/api-docs` |
| Ventas | `/ventas/facturas-venta` | `/ventas/v3/api-docs` |
| Auth | `/auth/login` | `/auth/v3/api-docs` |

`/ventas/facturas` is an alias for `/ventas/facturas-venta`.
`/medios-pago` and its child paths route to `/ventas/medios-pago`.
Compras retains `persona.idPersona` and `deposito.idDeposito` in nested responses;
Inventario and Ventas retain their canonical `id` fields. Pagination is preserved:
Compras and payment methods use `contenido`/`numeroPagina`/`tamanio`, Inventario
uses Spring Page fields, and the remaining Ventas lists use `content`/`pageNumber`/`pageSize`.

Auth's own Swagger UI is at <http://localhost:8084/auth/swagger-ui.html>. The
bootstrap admin comes from `AUTH_ADMIN_USERNAME` / `AUTH_ADMIN_PASSWORD`
(defaults `admin` / `Admin123!`, development only).

## Integration verification

After building, run:

```sh
python3 scripts/verify_stack.py
```

Requires Python 3, Java 21, and PostgreSQL binaries (`initdb`, `pg_ctl`, `psql`) on
PATH. Run as a regular user, since `initdb` cannot run as root. It starts a new
PostgreSQL cluster and all applications on available local ports, checks every
controller group, gateway routing, Swagger, auth login/token validation,
Compras 401/403 rules, validation, and compatibility, then verifies stock changes `0 → 10 → 7 → 10`
through purchase, sale, and cancellation. A full stack restart must preserve stock
and Liquibase history. Processes are stopped on exit; the temporary directory
printed at startup retains database files and logs. Inherited database settings are
replaced for this disposable run. Caches are disabled by default; pass `--cache`
to exercise Compras and Auth against a Redis running on `127.0.0.1:6379`.

When executable JAR packaging dependencies are unavailable but application
dependencies are cached, use compiled classes:

```sh
python3 scripts/build_stack.py --classes --offline --repository /tmp/distrimarket-m2
python3 scripts/verify_stack.py --classes
```

This fallback tests compiled applications with Maven's test classpath, including
provided and test dependencies. Verify normal executable JAR packaging once Maven
Central is accessible.
