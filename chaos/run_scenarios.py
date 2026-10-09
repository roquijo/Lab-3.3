#!/usr/bin/env python3
"""Escenarios de caos contra el stack local ya armado con CHAOS_ARMED=true.

Escribe evidence/chaos/resultados.json solo con lo que Prometheus y los
endpoints respondan. Sale con error si el stack no está listo.
"""

import json
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

BASE = "http://localhost:18080"
SERVICE_B = "http://localhost:18081"
DATA = "http://localhost:18082"
PROM = "http://localhost:19090"
TOKEN = "lab33-local-token"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "evidence" / "chaos" / "resultados.json"


def request(url, method="GET", headers=None, timeout=10):
    req = urllib.request.Request(url, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            body = response.read().decode("utf-8", errors="replace")
            return response.status, body
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode("utf-8", errors="replace")


def chaos(url, enable):
    status, body = request(
        url,
        method="POST",
        headers={"X-Lab-Token": TOKEN, "X-Lab-Enable": "true" if enable else "false"},
    )
    if status != 204:
        raise SystemExit(f"No se pudo cambiar el caos en {url}: HTTP {status} {body}")


def stop_all():
    for url in (f"{SERVICE_B}/api/chaos/stop", f"{DATA}/api/chaos/stop"):
        status, body = request(url, method="POST", headers={"X-Lab-Token": TOKEN})
        if status not in (204, 409):
            raise SystemExit(f"Apagado falló en {url}: HTTP {status} {body}")


def traffic(seconds, experiment=None):
    deadline = time.time() + seconds
    ok = err = 0
    header = {"X-Lab-Experiment": experiment} if experiment else {}
    while time.time() < deadline:
        status, _ = request(f"{BASE}/api/orders/ord-001", headers=header, timeout=8)
        if status == 200:
            ok += 1
        else:
            err += 1
        time.sleep(0.05)
    return {"ok": ok, "errors": err, "total": ok + err}


def firing(name):
    payload = json.load(urllib.request.urlopen(f"{PROM}/api/v1/alerts", timeout=10))
    return [alert for alert in payload["data"]["alerts"] if alert.get("state") == "firing" and alert["labels"].get("alertname") == name]


def wait_alert(name, timeout_s):
    started = time.time()
    while time.time() - started < timeout_s:
        found = firing(name)
        if found:
            return time.time() - started, found[0].get("activeAt")
        time.sleep(5)
    return None, None


def iso():
    return datetime.now(timezone.utc).isoformat()


def scenario(name, setup, experiment_header, watch, budget_seconds=150):
    stop_all()
    time.sleep(1)
    started_epoch = time.time()
    started = iso()
    setup()
    first_degradation = None
    samples_ok = samples_err = 0
    header = {"X-Lab-Experiment": experiment_header} if experiment_header else {}
    mttd = None
    active_at = None
    while time.time() - started_epoch < budget_seconds:
        t0 = time.perf_counter()
        status, _ = request(f"{BASE}/api/orders/ord-001", headers=header, timeout=8)
        elapsed_ms = (time.perf_counter() - t0) * 1000
        if status == 200:
            samples_ok += 1
        else:
            samples_err += 1
        if first_degradation is None and (status >= 500 or elapsed_ms >= 180):
            first_degradation = iso()
        found = firing(watch)
        if found:
            mttd = round(time.time() - started_epoch, 1)
            active_at = found[0].get("activeAt")
            break
        time.sleep(1)
    stop_all()
    return {
        "scenario": name,
        "started_at": started,
        "first_degradation_at": first_degradation,
        "alert": watch,
        "alert_active_at": active_at,
        "mttd_seconds": mttd,
        "requests_ok": samples_ok,
        "requests_error": samples_err,
    }


def main():
    status, _ = request(f"{BASE}/actuator/health/liveness")
    if status != 200:
        raise SystemExit("service-a no responde en http://localhost:18080")
    armed, body = request(f"{SERVICE_B}/api/chaos")
    if armed != 200 or '"armed":true' not in body.replace(" ", ""):
        raise SystemExit("service-b no está armado. Levanta el override deploy/local/docker-compose.chaos.yml")

    # Calentamiento para que existan series. No es un resultado.
    traffic(20)

    results = []
    t0 = time.time()
    results.append(scenario(
        "latencia-200ms",
        lambda: chaos(f"{SERVICE_B}/api/chaos/latency", True),
        None,
        "ServiceALatencySlo",
    ))
    # El mttd del bucle anterior mezcla el reloj. Se recalcula abajo de forma simple.
    results.append(scenario(
        "errores-10",
        lambda: chaos(f"{DATA}/api/chaos/errors", True),
        "error-rate",
        "DataServiceErrorStatic",
    ))
    results.append(scenario(
        "correlacion",
        lambda: (chaos(f"{SERVICE_B}/api/chaos/latency", True), chaos(f"{DATA}/api/chaos/errors", True)),
        "error-rate",
        "DataServiceCorrelatedAnomaly",
    ))
    stop_all()
    recovered, _ = request(f"{BASE}/api/orders/ord-001")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "generated_at": iso(),
        "elapsed_seconds": round(time.time() - t0, 1),
        "recovered_http": recovered,
        "slo_latency_seconds": 0.150,
        "slo_success": 0.99,
        "scenarios": results,
        "note": "mttd_seconds es el tiempo desde el inicio del sondeo hasta ver la alerta, o null si no apareció en la ventana.",
    }
    OUT.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(OUT.read_text(encoding="utf-8"))


if __name__ == "__main__":
    main()
