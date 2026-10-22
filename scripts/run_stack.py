#!/usr/bin/env python3
"""Start the local stack using the compiled runtime classpaths."""

import os
from pathlib import Path
import socket
import subprocess
import time
import urllib.error
import urllib.request


WORKSPACE = Path(__file__).resolve().parents[2]
SERVICES = (
    ("distrimarket-ms-featureB", "com.distrimarket.inventario.InventarioApplication", 8081, "/inventario/v3/api-docs"),
    ("distrimarket-ms-featureC", "com.distrimarket.ms.featurec.DistrimarketMsFeatureCApplication", 8082, "/ventas/v3/api-docs"),
    ("distrimarket-ms-featureA", "com.distrimarket.compras.ComprasApplication", 8083, "/v3/api-docs"),
    ("distrimarket-ms-auth", "com.distrimarket.ms.auth.DistrimarketMsAuthApplication", 8084, "/auth/v3/api-docs"),
    ("distrimarket-gateway", "com.distrimarket.gateway.GatewayApplication", 8080, "/v3/api-docs/swagger-config"),
)


def environment():
    env = os.environ.copy()
    path = WORKSPACE / "distrimarket-common" / ".env.local"
    if not path.exists():
        raise RuntimeError(f"Missing shared database settings: {path}")
    # Read literal KEY=value entries without evaluating passwords as shell code.
    for number, line in enumerate(path.read_text().splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        key, separator, value = line.partition("=")
        key = key.strip()
        if not separator or not key.isidentifier():
            raise RuntimeError(f"Invalid environment entry at {path}:{number}")
        value = value.strip()
        if value[:1] in ("'", '"'):
            if len(value) < 2 or value[-1] != value[0]:
                raise RuntimeError(f"Unclosed quoted value at {path}:{number}")
            value = value[1:-1]
        env[key] = value
    # Use the shared DB configuration even if the terminal has Spring overrides.
    for key in ("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD"):
        env.pop(key, None)
    return env


def port_open(port):
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=1):
            return True
    except OSError:
        return False


def request(path, port):
    with urllib.request.urlopen(f"http://localhost:{port}{path}", timeout=3) as response:
        return response.read()


def main():
    env = environment()
    # Check every missing service's classpath before starting any processes.
    for name, _, port, _ in SERVICES:
        if not port_open(port):
            cp = WORKSPACE / name / "target" / "runtime-classpath.txt"
            if not cp.is_file():
                raise RuntimeError(f"Missing {cp}; build with scripts/build_stack.py --classes first")
            if any(not Path(entry).exists() for entry in cp.read_text().strip().split(os.pathsep)):
                raise RuntimeError(f"Dependencies missing from {cp}; rebuild with --classes first")

    for name, main_class, port, readiness_path in SERVICES:
        repo = WORKSPACE / name
        log_path = repo / "target" / "local-run.log"
        process = None
        if port_open(port):
            print(f"Port {port} is already in use; checking {name}.", flush=True)
        else:
            classpath = (repo / "target" / "runtime-classpath.txt").read_text().strip()
            command = [
                "java", "-Xms64m", "-Xmx384m", "-XX:ActiveProcessorCount=2",
                "-Dspring.devtools.restart.enabled=false", "-cp", classpath, main_class,
                f"--server.port={port}",
            ]
            with log_path.open("a") as output:
                process = subprocess.Popen(command, cwd=repo, env=env, stdin=subprocess.DEVNULL,
                                           stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
            (repo / "target" / "local-run.pid").write_text(str(process.pid) + "\n")
            print(f"Starting {name}: PID {process.pid}; log {log_path}", flush=True)
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            if process is not None and process.poll() is not None:
                raise RuntimeError(f"{name} exited during startup; see {log_path}")
            try:
                request(readiness_path, port)
                print(f"Ready: {name} on port {port}", flush=True)
                break
            except (OSError, urllib.error.URLError):
                time.sleep(1)
        else:
            raise RuntimeError(f"{name} did not become ready; see {log_path}. Started processes remain running.")

    print("All services are ready.", flush=True)
    print("Swagger: http://localhost:8080/swagger-ui.html", flush=True)
    print("Auth Swagger: http://localhost:8084/auth/swagger-ui.html", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError) as error:
        raise SystemExit(str(error))
