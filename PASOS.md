# PASOS — cómo ejecutar y defender Lab 3.3

Esta guía usa los puertos y archivos de este repositorio. Los servicios publican 18080, 13000 y 19090 para no ocupar los puertos locales habituales 8080, 3000 y 9090.

Las conclusiones que hay que poder defender están en [docs/reporte-ejecutivo.pdf](docs/reporte-ejecutivo.pdf), en especial las secciones de evidencia, caos y madurez. Si una cifra no aparece ahí como medida, no la afirmes en el video.

## A. Preparación

Instala JDK 21, Maven 3.9, Docker Desktop y Python 3.11 o superior. Para la nube, más adelante: `gcloud`, `kubectl`, Terraform y AWS CLI. No hacen falta para la demostración local.

Comprueba:

```powershell
java -version
mvn -version
docker version
python --version
```

Copia `.env.example` a `.env` solo si quieres cambiar el token o la contraseña local. Los valores de ejemplo no son secretos de una cuenta real. No pongas claves de GCP ni de AWS en ese archivo.

Sin credenciales cloud puedes compilar, levantar Compose, ver Grafana y correr el caos local. Crear GKE, Cloud SQL, ECS, RDS, Flow Logs, Security Hub o Security Command Center gasta dinero y exige permisos. No lo hagas durante el ensayo si no tienes una cuenta de laboratorio y un tope de gasto.

## B. Ejecución local

Todos los comandos salen de la carpeta `Lab 3.3`.

### Paso 1. Probar la lógica sin contenedores

Objetivo: ver que el caos no se activa solo y que data-service lee el pedido.

Concepto: una prueba unitaria no demuestra el Collector. Demuestra la regla de negocio y las compuertas del experimento.

```powershell
mvn test
```

Resultado esperado: el build termina en éxito. Verás un log `latencia inyectada delayMs=200` dentro de la prueba de service-b, y `error inyectado` en la prueba que fuerza el aleatorio a cero. Eso no es el sistema en marcha: es el código bajo prueba.

Si Maven no encuentra Java 21, revisa `JAVA_HOME`.

### Paso 2. Construir y levantar el stack

Objetivo: tener los tres servicios, MySQL y el pipeline de telemetría.

Concepto: el agente Java vive en la imagen, no en el `pom.xml` de runtime. El Collector recibe OTLP y reparte trazas, métricas y logs.

```powershell
docker compose -f deploy/local/docker-compose.yaml up -d --build
docker compose -f deploy/local/docker-compose.yaml ps
```

Resultado esperado: contenedores `lab33-*` en ejecución. service-a publica el puerto 18080.

Comprobación:

```powershell
curl http://localhost:18080/api/orders/ord-001
curl http://localhost:18082/actuator/health/liveness
curl http://localhost:23133/
```

La respuesta del pedido debe traer `orderId` `ord-001`, SKU `LAPTOP-X1`, `stockSufficient` verdadero y un `traceId` de 32 hexadecimales. `latencyInjectedMs` debe ser 0.

Error habitual: Docker Hub responde 504 al pedir `maven:3.9.9-eclipse-temurin-21`. No es un fallo del código. Repite el comando cuando el registro responda. En la sesión del 9 de octubre de 2026 ocurrió ese 504 y el stack no llegó a arrancar.

Observa en `docker compose ps` que MySQL está healthy antes de data-service. Si data-service reinicia, mira `docker logs lab33-data-service`: el perfil activo tiene que ser `local`.

### Paso 3. Seguir una traza

Objetivo: ver la misma operación en los tres servicios.

Concepto: `traceparent` viaja en la llamada HTTP. El `trace_id` del JSON es el de la traza.

Abre http://localhost:13000, entra con admin / admin, carpeta Lab 3.3, tablero «Servicios y SLO». En Explore, datasource Tempo, busca el `traceId` que devolvió el curl.

Debes ver `order.coordinate`, `fulfillment.prepare`, `record.load` y un `SELECT` o el span JDBC del `JOIN`. Si Tempo responde 404 unos segundos, espera y repite: el ingester a veces lista la traza antes de poder devolverla por id.

En una terminal:

```powershell
docker logs lab33-service-a --tail 20
docker logs lab33-data-service --tail 20
```

Las dos líneas del pedido tienen que compartir `trace_id` y tener `span_id` distintos.

### Paso 4. Métricas

```powershell
curl "http://localhost:19090/api/v1/query?query=lab:service_a:latency_p99:2m"
```

Al principio la serie puede no existir: la regla graba cada 30 segundos y el agente exporta cada 15. Genera unas peticiones y espera un minuto. El valor sano debería quedar muy por debajo de 0,15 segundos. Si el panel de Grafana marca picos de segundos con poco tráfico, no lo cites como p99 de una prueba de carga: el histograma se deforma cuando hay pocas muestras.

## C. Demostración por módulo

### Arquitectura

Ejecuta el curl del paso 2. Muestra `deploy/local/docker-compose.yaml` y señala que service-a no tiene `SPRING_DATASOURCE_URL`. Di: la base la toca solo data-service.

### OpenTelemetry y SQL

En Tempo, señala el span JDBC debajo de `record.load`. Di: ese span lo abre el agente sobre el driver, no un span manual con el texto del SQL.

### Correlación

En Grafana → Explore → Loki:

```logql
{service_name="data-service"} |= "Registro leído"
```

El cuerpo debe empezar por `trace_id=`. El campo derivado abre Tempo. Si el enlace no aparece, revisa que el datasource Loki tenga la expresión `trace_id=([0-9a-f]{32})`. En el YAML el dólar está escapado como `$$` porque Grafana se come un solo `$` al aprovisionar.

### Anomalías

Aún no las enciendas. Muestra `observability/prometheus/rules/lab33-alerts.yml`. Explica en voz alta:

- error rate = 5xx de `/api/records/` dividido entre todas las peticiones de esa ruta, en 2 minutos;
- baseline = promedio de esa serie en 10 minutos;
- sigma = `stddev_over_time` de la misma ventana;
- la alerta correlacionada además exige p99 de service-a mayor que 150 ms y al menos 8 puntos, para no disparar en el arranque;
- el `trace_id` no está en la métrica. Se recupera con `python observability/aiops/enrich_incident.py`.

### Seguridad

```powershell
curl -Method POST -ContentType "application/json" -Body '{"token":"incorrecto"}' http://localhost:18080/api/session
```

Esperado: HTTP 401. En los logs, la frase `autenticacion rechazada` y ningún rastro del token enviado. El tablero «Golden signals de seguridad» debe mover la serie de 401. El panel de texto dice que no hay CVE: no lo reemplaces por un número.

### Latencia de 200 ms

Primero arma el proceso. Esto recrea service-b y data-service con `CHAOS_ARMED=true`. Sigue sin inyectar nada hasta el POST.

```powershell
docker compose -f deploy/local/docker-compose.yaml -f deploy/local/docker-compose.chaos.yml up -d --no-deps --force-recreate service-b data-service
curl http://localhost:18081/api/chaos
```

`armed` tiene que ser true y `latencyMs` 0.

```powershell
curl -Method POST -Headers @{"X-Lab-Token"="lab33-local-token"; "X-Lab-Enable"="true"} http://localhost:18081/api/chaos/latency
curl http://localhost:18080/api/orders/ord-001
```

`latencyInjectedMs` debe ser 200 y la llamada tardar algo más de 200 ms. En Tempo, el span `fulfillment.prepare` lleva `chaos.delay_ms=200`. service-a se alarga porque espera a service-b. data-service no duerme.

Apagado:

```powershell
curl -Method POST -Headers @{"X-Lab-Token"="lab33-local-token"} http://localhost:18081/api/chaos/stop
curl http://localhost:18080/api/orders/ord-001
```

`latencyInjectedMs` vuelve a 0.

Si el POST responde 409, el contenedor no se recreó con el override. Si responde 401, el token no coincide con `LAB_API_TOKEN`.

### Error del 10 %

```powershell
curl -Method POST -Headers @{"X-Lab-Token"="lab33-local-token"; "X-Lab-Enable"="true"} http://localhost:18082/api/chaos/errors
```

Sin cabecera de experimento el pedido sigue en 200. Con la cabecera, cerca de una de cada diez llamadas falla:

```powershell
1..20 | ForEach-Object { curl -Headers @{"X-Lab-Experiment"="error-rate"} -SkipHttpErrorCheck http://localhost:18080/api/orders/ord-001 | Out-Null; $_.StatusCode }
```

En PowerShell 5, `curl` es `Invoke-WebRequest` y el estado se ve con un bloque try. No hace falta un porcentaje exacto en veinte muestras. Cuenta 5xx frente a 200 y di que la probabilidad configurada es 0,10, no el porcentaje de una muestra corta.

Apagado:

```powershell
curl -Method POST -Headers @{"X-Lab-Token"="lab33-local-token"} http://localhost:18082/api/chaos/stop
```

Una llamada con la cabecera debe volver a 200.

### MTTD, SLO y presupuesto

El script `chaos/run_scenarios.py` hace tres pasadas: solo latencia, solo errores, y las dos juntas. Mide el reloj hasta que Prometheus muestra la alerta esperada, con un tope de 150 segundos. Escribe `evidence/chaos/resultados.json` únicamente si el stack responde.

Antes, el stack tiene que estar armado (el override) y haber tenido tráfico sano unos minutos, porque la alerta correlacionada exige historia.

```powershell
python chaos/run_scenarios.py
```

El inicio es el timestamp `started_at`. La detección es `alert_active_at` o, si prefieres el reloj del script, `mttd_seconds`. Si `mttd_seconds` es null, la alerta no apareció dentro de la ventana: no lo sustituyas por 120 segundos.

SLO de latencia: p99 de `/api/orders/` menor o igual a 150 ms. SLO de éxito: 99 % de 2xx. El presupuesto de una ventana es el 1 % de las peticiones. `ErrorBudget.consumedFraction` divide los fallos por ese presupuesto. La extrapolación a 30 días está en `extrapolatedFraction` y no es lo mismo que el consumo de los dos minutos del experimento.

### Alertas estáticas y dinámicas

En la misma corrida, la latencia sola debe poder encender `ServiceALatencySlo` y no la correlacionada, porque el error rate sigue cerca de cero. Los errores solos deben poder encender `DataServiceErrorStatic` sin cruzar los 150 ms si data-service responde rápido. Las dos juntas son las que cumplen la regla `baseline + 2σ` y el p99. Cuenta las alertas en el JSON. Si una no se comporta así, dilo: no calcules un porcentaje de reducción que el archivo no traiga.

### Madurez

Abre `docs/madurez.md` y las secciones 9 y 10 de [docs/reporte-ejecutivo.pdf](docs/reporte-ejecutivo.pdf). Los niveles están bajos a propósito donde falta una corrida. No subas un dominio a 4 en la sustentación solo porque el YAML existe. La sección 11 del informe recoge las mismas conclusiones: lo medido es la prueba unitaria; el MTTD del stack y el porcentaje de ruido de alertas siguen pendientes.

## D. GCP y AWS

No ejecutes `terraform apply` en el ensayo. El orden, cuando tengas proyecto, facturación y permiso, es:

1. `terraform -chdir=terraform/gcp init` y `plan` con un `terraform.tfvars` que no se sube a git. `create_cloud_sql` déjalo en false hasta revisar el costo de Cloud SQL.
2. Aplicar red, GKE y Artifact Registry. Construir las tres imágenes y subirlas.
3. Crear el secreto `mysql-credentials` a partir de `deploy/gcp/k8s/mysql-secret.example.yaml`. No uses el ejemplo tal cual.
4. `kubectl apply -k deploy/gcp/k8s`. El MySQL del clúster usa el perfil `local`. No lo llames Cloud SQL.
5. Cloud Service Mesh queda fuera del kustomize. Léelo en `deploy/gcp/cloud-service-mesh.md` antes de etiquetar el namespace.

En AWS:

1. `terraform -chdir=terraform/aws init` y `plan`. Hacen falta `data_service_image` y `lab_api_token`. `create_rds` en false deja MySQL en la tarea.
2. La tarea usa el perfil Spring `aws` y `RDS_JDBC_URL`. Si no creaste RDS, esa URL apunta al sidecar, no a un RDS simulado desde tu portátil.
3. App Mesh no va. Service Connect está explicado en `deploy/aws/service-connect.md` y no está en la service de ECS.
4. Los flow logs de la VPC sí están en `terraform/aws/flow_logs.tf`. Al destruir, `terraform destroy` borra también el log group. Revisa que no haya otra carga en la misma VPC.

Destruir:

```powershell
terraform -chdir=terraform/gcp destroy
terraform -chdir=terraform/aws destroy
```

Eso borra clúster, bases y logs de esa configuración. No lo lances si la cuenta no es la del laboratorio.

## E. Guion de video

Ensayo, 10 minutos, sin grabar: `mvn test`, compose en verde, un pedido 200, caos armado, una llamada lenta, una llamada con error, los dos POST de stop, y un pedido otra vez en 200 con `latencyInjectedMs` 0. Si Docker Hub falla, no grabes el tramo de contenedores: graba las pruebas y di que el registro no entregó las imágenes.

| Escena | Tiempo | Qué mostrar | Qué decir |
|---|---|---|---|
| 1 | 1 min | `docs/arquitectura.md` y el diagrama | Tres servicios, una sola base, perfiles distintos para local, GCP y AWS |
| 2 | 2 min | `mvn test` y el curl del pedido | El flujo real y el traceId |
| 3 | 2 min | Tempo y un log en Loki | El mismo trace_id, spans distintos, SQL del agente |
| 4 | 2 min | Regla de Prometheus y el tablero | baseline, sigma, SLO de 150 ms, por qué hace falta historia |
| 5 | 1 min | POST de sesión incorrecta y el tablero de seguridad | 401 real, sin token en el log, sin CVE inventado |
| 6 | 3 min | Activar latencia, curl, traza, stop | 200 ms, efecto en service-a, vuelta a cero |
| 7 | 3 min | Activar errores con cabecera, varias llamadas, stop | 10 % solo en peticiones marcadas |
| 8 | 2 min | `evidence/chaos/resultados.json` si existe | MTTD medido o, si es null, que no se alcanzó y no se inventa. Debe coincidir con la sección 8 del informe |
| 9 | 2 min | `docs/madurez.md` y las secciones 9 y 10 del informe | Niveles, brechas y la primera quincena del roadmap |

Si el JSON de evidencia no existe, la escena 8 enseña el script y dice que esta corrida no produjo el archivo.

## F. Material de estudio

Los tres pilares: la métrica dice cuánto y con qué frecuencia, el log dice qué pasó en un punto, la traza dice el camino. El `trace_id` los junta; no basta con imprimirlo si Grafana no tiene el derived field.

El agente es el SDK en el proceso. El Collector no instrumenta la aplicación: recibe, limita memoria, filtra salud, agrupa y exporta.

`baseline + 2σ`: si durante 10 minutos el error rate fue casi cero, la desviación es casi cero y cualquier subida clara queda por encima. Por eso la regla también pide el p99 y varias muestras. Un solo pico no debería bastar.

SLI de este lab: fracción de 2xx en `/api/orders/`. SLO: 99 %. Presupuesto: 1 % de las peticiones de la ventana. MTTD: desde que se enciende el experimento hasta que la alerta está `firing`.

Preguntas probables:

- ¿Por qué no App Mesh? Porque el soporte terminó el 30 de septiembre de 2026. La alternativa en ECS es Service Connect y no está desplegada.
- ¿Por qué el error no afecta a un usuario normal? Porque exige proceso armado, interruptor encendido y cabecera `X-Lab-Experiment: error-rate`.
- ¿El MySQL del Compose es Cloud SQL? No. Cloud SQL solo existe si `create_cloud_sql` es true y el perfil es `gcp`.
- ¿Dónde está el trace_id de la alerta? No en PromQL. En el log, vía `enrich_incident.py`.

Ejercicio: con el caos desarmado, intenta el POST de latencia y explica el 409. Luego arma el proceso, enciende la latencia, copia el `traceId` y encuéntralo en Loki.

## G. Checklist

- [ ] `mvn test` en verde
- [ ] `CHAOS_ARMED` en false en el compose base
- [ ] Pedido `ord-001` en 200 con `latencyInjectedMs` 0
- [ ] Traza con service-a, service-b, data-service y JDBC
- [ ] Log sin token ni contraseña
- [ ] Latencia 200 ms activada y apagada
- [ ] Errores solo con la cabecera, y apagados después
- [ ] `resultados.json` presente o declarado como no generado
- [ ] Tablero de seguridad sin un número de CVE
- [ ] Ningún `terraform apply` accidental
- [ ] `docker compose -f deploy/local/docker-compose.yaml down` al terminar el ensayo local

`down -v` borra también la base local.
