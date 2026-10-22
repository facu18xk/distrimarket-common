#!/usr/bin/env python3
"""Verify gateway contracts, auth, and purchase/sale stock effects with disposable PostgreSQL."""
import argparse
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

WORKSPACE = Path(__file__).resolve().parents[2]
APPLICATIONS = {
    "gateway": ("distrimarket-gateway", "com.distrimarket.gateway.GatewayApplication"),
    "compras": ("distrimarket-ms-featureA", "com.distrimarket.compras.ComprasApplication"),
    "inventario": ("distrimarket-ms-featureB", "com.distrimarket.inventario.InventarioApplication"),
    "ventas": ("distrimarket-ms-featureC", "com.distrimarket.ms.featurec.DistrimarketMsFeatureCApplication"),
    "auth": ("distrimarket-ms-auth", "com.distrimarket.ms.auth.DistrimarketMsAuthApplication"),
}


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def http(url, method="GET", body=None, expected=200, headers=None):
    encoded = None if body is None else json.dumps(body).encode()
    request = Request(url, encoded, {"Accept": "application/json", "Content-Type": "application/json", **(headers or {})}, method=method)
    try:
        response = urlopen(request, timeout=20)
    except HTTPError as error:
        response = error
    with response:
        data = response.read().decode()
        if response.status != expected:
            raise AssertionError(f"{method} {url}: expected {expected}, got {response.status}: {data[:1200]}")
        if not data:
            return None
        if response.headers.get("Content-Type", "").startswith("application/json"):
            return json.loads(data)
        return data


def eventually(check, timeout=120):
    end = time.monotonic() + timeout
    last = None
    while time.monotonic() < end:
        try:
            return check()
        except (AssertionError, URLError, TimeoutError, ConnectionError) as error:
            last = error
            time.sleep(1)
    raise AssertionError(f"Timed out: {last}")


def require(condition, message):
    if not condition:
        raise AssertionError(message)


class Stack:
    def __init__(self, directory, classes, cache=False):
        self.directory = directory
        self.classes = classes
        self.cache = cache
        self.ports = {}
        for name in [*APPLICATIONS, "postgres"]:
            port = free_port()
            while port in self.ports.values():
                port = free_port()
            self.ports[name] = port
        self.processes = []
        self.files = []
        self.pgdata = directory / "postgres"
        self.pg_started = False

    def start_database(self):
        for binary in ["initdb", "pg_ctl", "psql", "java"]:
            if not shutil.which(binary):
                raise RuntimeError("Required executable not found: " + binary)
        subprocess.run(["initdb", "-D", str(self.pgdata), "-U", "postgres", "-A", "trust", "--no-locale", "--encoding=UTF8"], check=True, stdout=subprocess.DEVNULL)
        subprocess.run(["pg_ctl", "-D", str(self.pgdata), "-l", str(self.directory / "postgres.log"), "-o",
                        f"-h 127.0.0.1 -p {self.ports['postgres']} -k {self.directory}", "-w", "start"], check=True, stdout=subprocess.DEVNULL)
        self.pg_started = True

    def sql(self, query):
        return subprocess.check_output(["psql", "-h", "127.0.0.1", "-p", str(self.ports["postgres"]), "-U", "postgres", "-d", "postgres", "-At", "-c", query], text=True).strip()

    def start_service(self, name):
        repo, main = APPLICATIONS[name]
        directory = WORKSPACE / repo
        env = dict(os.environ)
        # Ignore inherited production connection configuration.
        for key in list(env):
            if key.startswith(("SPRING_", "DB_", "OPENAPI_")) or key == "JAVA_TOOL_OPTIONS":
                del env[key]
        env.update({"SERVER_PORT": str(self.ports[name]), "PORT": str(self.ports[name]),
                    "SPRING_DATASOURCE_URL": f"jdbc:postgresql://127.0.0.1:{self.ports['postgres']}/postgres?sslmode=disable&prepareThreshold=0",
                    "DB_USER": "postgres", "DB_PASSWORD": "", "LIQUIBASE_ENABLED": "true",
                    "CACHE_ENABLED": "true" if self.cache else "false",
                    "REDIS_HOST": "127.0.0.1", "REDIS_PORT": "6379",
                    "SPRING_JPA_SHOW_SQL": "false",
                    "JWT_SECRET": "distrimarket-dev-secret-cambiar-en-produccion-2026",
                    "JWT_ISSUER": "distrimarket-auth",
                    "AUTH_BOOTSTRAP_ADMIN": "true", "AUTH_ADMIN_USERNAME": "admin",
                    "AUTH_ADMIN_PASSWORD": "Admin123!",
                    "COMPRAS_URL": f"http://127.0.0.1:{self.ports['compras']}",
                    "INVENTARIO_URL": f"http://127.0.0.1:{self.ports['inventario']}",
                    "VENTAS_URL": f"http://127.0.0.1:{self.ports['ventas']}",
                    "AUTH_URL": f"http://127.0.0.1:{self.ports['auth']}",
                    "LOG_LEVEL_ROOT": "WARN", "LOG_LEVEL_APP": "WARN", "LOG_LEVEL_REPOSITORY": "WARN"})
        command = ["java", "-Xms64m", "-Xmx256m", "-XX:ActiveProcessorCount=2"]
        if self.classes:
            command += ["-cp", directory.joinpath("target/runtime-classpath.txt").read_text().strip(), main]
        else:
            jars = [p for p in directory.glob("target/*.jar") if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
            require(len(jars) == 1, f"Expected one executable JAR in {directory}/target")
            command += ["-jar", str(jars[0])]
        output = (self.directory / (name + ".log")).open("a")
        self.files.append(output)
        process = subprocess.Popen(command, cwd=self.directory, env=env, stdout=output, stderr=subprocess.STDOUT)
        self.processes.append((name, process))
        path = "/openapi.yaml" if name == "gateway" else ("" if name == "compras" else "/" + name) + "/v3/api-docs"
        def ready():
            if process.poll() is not None:
                raise RuntimeError(name + " exited; see " + str(self.directory / (name + ".log")))
            return http(f"http://127.0.0.1:{self.ports[name]}" + path)
        eventually(ready)
        print(name + " started", flush=True)

    def stop_services(self):
        for _, process in reversed(self.processes):
            process.terminate()
        for _, process in reversed(self.processes):
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        self.processes.clear()
        for output in self.files:
            output.close()
        self.files.clear()

    def close(self):
        self.stop_services()
        if self.pg_started:
            subprocess.run(["pg_ctl", "-D", str(self.pgdata), "-m", "fast", "-w", "stop"], check=True, stdout=subprocess.DEVNULL)


def login_token(gateway, username, password):
    return http(gateway + "/auth/login", method="POST",
                body={"username": username, "password": password})


def verify(stack):
    gateway = f"http://127.0.0.1:{stack.ports['gateway']}"
    token = login_token(gateway, "admin", "Admin123!")["accessToken"]

    def api(path, **kwargs):
        headers = {"Authorization": "Bearer " + token}
        headers.update(kwargs.pop("headers", {}))
        return http(gateway + path, headers=headers, **kwargs)

    # Compras is protected; the rest are still open in this iteration.
    http(gateway + "/compras/facturas", expected=401)
    routes = {
        "inventario": ["categorias", "marcas", "productos", "depositos", "stock-depositos", "personas", "empleados", "ajustes-stock", "transferencias-stock"],
        "compras": ["facturas", "proveedores", "ordenes", "proveedores-productos"],
        "ventas": ["clientes", "timbrados", "medios-pago", "facturas-venta", "facturas"],
    }
    for service, groups in routes.items():
        for group in groups:
            result = eventually(lambda: api(f"/{service}/{group}?page=0&size=2&sort=id,asc"))
            require(isinstance(result, dict), service + "/" + group + " must return a page")
            expected = "contenido" if service == "compras" or group == "medios-pago" else "content"
            require(expected in result, service + "/" + group + " pagination changed")
    print("All controller groups passed", flush=True)
    spec = api("/openapi.yaml", headers={"Accept": "application/yaml"})
    require("url: http://localhost:8080" in spec, "Swagger must use the gateway server")
    require("/ventas/facturas-venta:" in spec and "/compras/facturas:" in spec and "/inventario/productos:" in spec
            and "/auth/login:" in spec, "Shared contract must include all services")
    http(gateway + "/swagger-ui.html", headers={"Accept": "text/html"})
    config = api("/v3/api-docs/swagger-config")
    require(config["url"] == "/openapi.yaml", "Gateway Swagger must use the shared contract")
    for service in routes:
        doc = api("/" + service + "/v3/api-docs")
        require(bool(doc.get("paths")), service + " documentation must route through the gateway")
    auth_doc = api("/auth/v3/api-docs")
    require(bool(auth_doc.get("paths")), "Auth documentation must route through the gateway")
    login = api("/auth/login", method="POST", body={"username": "admin", "password": "Admin123!"})
    require(bool(login.get("accessToken")), "Auth login must return a token")
    me = api("/auth/me", headers={"Authorization": "Bearer " + login["accessToken"]})
    require(me.get("username") == "admin", "Auth token must identify the bootstrap admin")
    print("Auth login and token validation passed", flush=True)

    def create(path, body):
        return api(path, method="POST", body=body, expected=201)
    person = {"tipoPersona": "FISICA", "nombreCompleto": "Integration Employee", "ci": "INTEGRATION-1"}
    categoria = create("/inventario/categorias", {"nombre": "Integration"})
    marca = create("/inventario/marcas", {"nombre": "Integration"})
    deposito = create("/inventario/depositos", {"nombre": "Integration", "ubicacion": "Local", "estado": True})
    empleado = create("/inventario/empleados", {"cargo": "Cajero", "fechaIngreso": "2026-01-01", "estado": True, "persona": person})
    require(empleado["persona"]["id"] > 0, "Inventario must retain nested persona.id")
    create("/auth/usuarios", {"username": "vendedor_test", "password": "Vendedor123!",
                              "idEmpleado": empleado["id"], "roles": ["VENDEDOR"]})
    vendedor = login_token(gateway, "vendedor_test", "Vendedor123!")
    vendedor_headers = {"Authorization": "Bearer " + vendedor["accessToken"]}
    http(gateway + "/compras/facturas", headers=vendedor_headers, expected=403)
    producto = create("/inventario/productos", {"idCategoria": categoria["id"], "idMarca": marca["id"], "codigoBarra": "INTEGRATION-SKU",
                                               "nombre": "Integration Product", "precioCosto": 50, "precioVenta": 100, "porcentajeIva": 10, "stockMinimo": 1, "estado": True})
    medio = create("/ventas/medios-pago", {"nombre": "Integration Cash"})
    timbrado = create("/ventas/timbrados", {"numeroTimbrado": "INTEGRATION", "fechaInicio": "2026-01-01", "fechaVencimiento": "2099-12-31", "sucursal": "001", "puntoExpedicion": "001", "activo": True})
    proveedor = create("/compras/proveedores", {"nombreFantasia": "Integration Supplier", "persona": {"tipoPersona": "JURIDICA", "nombreCompleto": "Integration Supplier", "ruc": "INTEGRATION-RUC"}})
    require(proveedor["persona"]["idPersona"] > 0, "Compras must retain nested persona.idPersona")
    cliente = create("/ventas/clientes", {"persona": {"tipoPersona": "FISICA", "nombreCompleto": "Integration Customer", "ci": "INTEGRATION-2"}})
    compra = create("/compras/facturas", {"numeroFactura": "001-001-0000001", "fechaEmision": "2026-10-10", "idProveedor": proveedor["idProveedor"],
        "idDeposito": deposito["id"], "idMedioPago": medio["idMedioPago"], "timbrado": "SUPPLIER", "condicionCompra": "CONTADO",
        "detalles": [{"idProducto": producto["id"], "cantidad": 10, "precioUnitario": 50, "porcentajeIva": 10}]})
    require(compra["deposito"]["idDeposito"] == deposito["id"], "Compras must retain nested deposito.idDeposito")
    stock_path = f"/inventario/stock-depositos/deposito/{deposito['id']}/producto/{producto['id']}"
    require(api(stock_path)["cantidad"] == 10, "Purchase must add ten units")
    sale_request = {"numeroFactura": "001-001-0000002", "fechaEmision": "2026-10-10", "idCliente": cliente["idCliente"],
        "idEmpleado": empleado["id"], "idDeposito": deposito["id"], "idMedioPago": medio["idMedioPago"], "idTimbrado": timbrado["idTimbrado"],
        "detalles": [{"idProducto": producto["id"], "cantidad": 3}]}
    venta = create("/ventas/facturas-venta", sale_request)
    require(venta["empleado"]["persona"]["id"] == empleado["persona"]["id"], "Sale must serialize the shared employee")
    require(api(stock_path)["cantidad"] == 7, "Sale must subtract three units")
    invoice_path = f"/ventas/facturas/{venta['idFacturaVenta']}"
    require(api(invoice_path)["idFacturaVenta"] == venta["idFacturaVenta"], "Invoice alias must preserve IDs")
    api(invoice_path, method="PATCH", body={"estado": "ANULADA"})
    require(api(stock_path)["cantidad"] == 10, "Cancellation must restore stock")
    api(invoice_path, method="PATCH", body={"estado": "ANULADA"})
    require(api(stock_path)["cantidad"] == 10, "Repeated cancellation must not restore stock twice")
    require(api(f"/medios-pago/{medio['idMedioPago']}")["idMedioPago"] == medio["idMedioPago"], "Payment alias must route to Ventas")
    api("/ventas/clientes/99999999", expected=404)
    api("/compras/proveedores", method="POST", body={}, expected=400)
    api("/inventario/productos", method="POST", body={}, expected=400)
    api("/ventas/facturas-venta", method="POST", body={}, expected=400)
    page = api("/ventas/clientes?page=0&size=1&q=Integration")
    require(page["pageSize"] == 1 and len(page["content"]) == 1, "Gateway must preserve query parameters")
    print("Swagger, API compatibility, validation, and stock 0 → 10 → 7 → 10 passed", flush=True)
    return stock_path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classes", action="store_true", help="Use classpaths produced by build_stack.py --classes")
    parser.add_argument("--cache", action="store_true",
                        help="Enable Redis caches; requires a Redis server on 127.0.0.1:6379")
    args = parser.parse_args()
    if args.cache:
        try:
            with socket.create_connection(("127.0.0.1", 6379), timeout=2):
                pass
        except OSError as error:
            raise SystemExit("Redis is not reachable on 127.0.0.1:6379: " + str(error))
    directory = Path(tempfile.mkdtemp(prefix="distrimarket-stack-"))
    print("Disposable database and logs: " + str(directory), flush=True)
    stack = Stack(directory, args.classes, args.cache)
    try:
        stack.start_database()
        for name in APPLICATIONS:
            stack.start_service(name)
        stock_path = verify(stack)
        migrations = stack.sql("SELECT string_agg(id || ':' || md5sum, ',' ORDER BY orderexecuted) FROM databasechangelog")
        stack.stop_services()
        for name in APPLICATIONS:
            stack.start_service(name)
        gateway = f"http://127.0.0.1:{stack.ports['gateway']}"
        token = login_token(gateway, "admin", "Admin123!")["accessToken"]
        require(eventually(lambda: http(gateway + stock_path,
                                        headers={"Authorization": "Bearer " + token}))["cantidad"] == 10,
                "Restart must preserve stock")
        require(stack.sql("SELECT string_agg(id || ':' || md5sum, ',' ORDER BY orderexecuted) FROM databasechangelog") == migrations, "Restart must not change migration history")
        print("Restart and Liquibase history passed. Full integration verification succeeded.", flush=True)
    finally:
        stack.close()


if __name__ == "__main__":
    main()
