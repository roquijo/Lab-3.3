#!/usr/bin/env python3
"""Lee alertas firing de Prometheus y, si hay un log de error, adjunta su trace_id.

No inventa un identificador. Si Loki no tiene una línea, el incidente lo dice.
"""

import json
import sys
import urllib.parse
import urllib.request
from datetime import datetime, timezone

PROMETHEUS = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:19090"
LOKI = sys.argv[2] if len(sys.argv) > 2 else "http://localhost:13100"


def get(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=10) as response:
        return json.load(response)


def main() -> None:
    alerts = get(f"{PROMETHEUS}/api/v1/alerts")["data"]["alerts"]
    firing = [alert for alert in alerts if alert.get("state") == "firing"]
    now_ns = int(datetime.now(timezone.utc).timestamp() * 1_000_000_000)
    start_ns = now_ns - 15 * 60 * 1_000_000_000
    incidents = []
    for alert in firing:
        labels = alert.get("labels", {})
        query = '{service_name=~"service-a|service-b|data-service"} |~ "error inyectado|latencia inyectada|Fallo"'
        encoded = urllib.parse.urlencode({
            "query": query,
            "start": str(start_ns),
            "end": str(now_ns),
            "limit": "5",
        })
        loki = get(f"{LOKI}/loki/api/v1/query_range?{encoded}")
        trace_id = None
        line = None
        for stream in loki.get("data", {}).get("result", []):
            for value in stream.get("values", []):
                text = value[1]
                marker = "trace_id="
                if marker in text:
                    candidate = text.split(marker, 1)[1].split(" ", 1)[0].strip('"')
                    if len(candidate) == 32:
                        trace_id = candidate
                        line = text
                        break
            if trace_id:
                break
        incidents.append({
            "incident_id": labels.get("alertname", "alerta") + "-" + str(alert.get("activeAt", "")),
            "service": labels.get("service"),
            "environment": labels.get("environment", "local"),
            "platform": labels.get("platform", "local"),
            "anomaly_type": labels.get("anomaly_type"),
            "severity": labels.get("severity"),
            "active_at": alert.get("activeAt"),
            "annotations": alert.get("annotations", {}),
            "trace_id": trace_id,
            "trace_status": "encontrado en Loki" if trace_id else "sin traza relacionada en los logs de los últimos 15 minutos",
            "tempo_url": f"http://localhost:13200/api/traces/{trace_id}" if trace_id else None,
            "sample_log": line,
        })
    json.dump({"generated_at": datetime.now(timezone.utc).isoformat(), "firing": len(firing), "incidents": incidents}, sys.stdout, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
