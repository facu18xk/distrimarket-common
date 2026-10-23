# Distrimarket Common

## Cómo ejecutar

Con los seis repositorios en la misma carpeta, desde `distrimarket-common`:

```sh
cp .env.example .env.local
```

Configurar `DB_URL`, `DB_USER` y `DB_PASSWORD` en `.env.local`.

```sh
docker compose -f infra/docker-compose.yml up -d   # Redis (opcional)
python3 scripts/build_stack.py --classes
python3 scripts/run_stack.py
```

Sin Redis: agregar `CACHE_ENABLED=false` a `.env.local`.

**Swagger:** http://localhost:8080/swagger-ui.html

**Login de desarrollo:** `POST /auth/login` — `admin` / `Admin123!`.

## Detener

```sh
for d in distrimarket-gateway/eureka-server distrimarket-ms-featureB \
         distrimarket-ms-featureC distrimarket-ms-featureA distrimarket-ms-auth \
         distrimarket-gateway; do
  [ -f "$d/target/local-run.pid" ] && kill "$(cat "$d/target/local-run.pid")" 2>/dev/null || true
done
docker compose -f infra/docker-compose.yml down
```
