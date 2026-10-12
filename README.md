# Lab 3.3 — Observabilidad, SRE y caos

Tres servicios Spring Boot. service-a coordina un pedido, service-b puede inyectar 200 ms y data-service lee MySQL. OpenTelemetry, el Collector, Prometheus, Grafana, Tempo y Loki forman el camino local. GCP y AWS están en Terraform y no se desplegaron.

## Puertos en el host

Están elegidos para no ocupar los puertos locales habituales 8080, 3000 y 9090.

| Servicio | URL |
|---|---|
| service-a | http://localhost:18080/api/orders/ord-001 |
| service-b | http://localhost:18081/api/chaos |
| data-service | http://localhost:18082/api/chaos |
| Grafana | http://localhost:13000 (admin / admin) |
| Prometheus | http://localhost:19090 |
| Tempo | http://localhost:13200 |
| Loki | http://localhost:13100 |

## Arranque

Desde esta carpeta, cuando Docker pueda descargar `maven:3.9.9-eclipse-temurin-21` y `eclipse-temurin:21-jre-jammy`:

```powershell
docker compose -f deploy/local/docker-compose.yaml up -d --build
mvn test
```

`mvn test` no necesita Docker. En esta máquina pasó. El `docker compose` de esta sesión falló con HTTP 504 de Docker Hub al pedir el token de las imágenes base.

## Documentos

| Archivo | Contenido |
|---|---|
| `docs/reporte-ejecutivo.pdf` | Informe ejecutivo, con el estado real de lo ejecutado y lo pendiente |
| `docs/arquitectura.md` | Flujo, perfiles de base y regla de anomalía |
| `docs/madurez.md` | Ocho dominios y roadmap de 90 días |
