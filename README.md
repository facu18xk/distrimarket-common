# Distrimarket Common

Librería compartida de los microservicios de Distrimarket. Contiene entidades, DTOs, seguridad, migraciones de base de datos y contratos OpenAPI. También incluye scripts para compilar y ejecutar el proyecto.

## Requisitos

- Java 21, Maven 3.9+ y Python 3.
- PostgreSQL en Supabase (u otro servidor). Docker para Redis (opcional).
- Tener estos repositorios en la misma carpeta: `distrimarket-common`, `distrimarket-gateway`, `distrimarket-ms-featureA`, `distrimarket-ms-featureB`, `distrimarket-ms-featureC` y `distrimarket-ms-auth`.

## Cómo ejecutar

Desde `distrimarket-common`:

```sh
cp .env.example .env.local
```

Configurar `DB_URL`, `DB_USER` y `DB_PASSWORD` en `.env.local`. Después:

```sh
docker compose -f infra/docker-compose.yml up -d   # Redis (opcional)
python3 scripts/build_stack.py --classes
python3 scripts/run_stack.py
```

Si no se usa Redis, configurar `CACHE_ENABLED=false` en `.env.local`. Si Maven no está en el PATH, definir `MAVEN=/ruta/a/mvn`.

| Servicio | Puerto |
| --- | --- |
| Eureka | 8761 |
| Gateway | 8080 |
| Inventario | 8081 |
| Ventas | 8082 |
| Compras | 8083 |
| Auth | 8084 |

**Swagger:** http://localhost:8080/swagger-ui.html

**Login de desarrollo:** `POST /auth/login` — `admin` / `Admin123!`.

### Detener

```sh
for d in distrimarket-gateway/eureka-server distrimarket-ms-featureB \
         distrimarket-ms-featureC distrimarket-ms-featureA distrimarket-ms-auth \
         distrimarket-gateway; do
  [ -f "$d/target/local-run.pid" ] && kill "$(cat "$d/target/local-run.pid")" 2>/dev/null || true
done
docker compose -f infra/docker-compose.yml down
```

## Verificar

```sh
python3 scripts/verify_stack.py
```

La verificación usa una base de datos temporal, sin modificar Supabase. Requiere `initdb`, `pg_ctl` y `psql` instalados.
