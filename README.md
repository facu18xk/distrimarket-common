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

El pooler de Supabase en modo sesión permite 15 clientes en total. Cada servicio usa
`DB_POOL_MAX_SIZE=3` y recicla conexiones con `DB_MAX_LIFETIME_MS` / `DB_KEEPALIVE_MS`
para que no queden sesiones ociosas colgadas.

Eureka es obligatorio: `run_stack.py` lo arranca primero (8761), después los servicios
y el gateway. Los servicios se registran como `localhost` (`EUREKA_INSTANCE_HOSTNAME`)
para que un cambio de IP de la máquina no deje las rutas `lb://` apuntando a una IP vieja.
En un despliegue multi-host, definir el hostname real o `EUREKA_PREFER_IP_ADDRESS=true`.

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
