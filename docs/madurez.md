# Madurez de observabilidad

No hay en este repositorio un documento llamado Observability Foundation Blueprint con ocho dominios oficiales. La búsqueda pública tampoco devolvió un marco con ese nombre y esa lista. La escala usada aquí va de 1 a 5 y se aplica a ocho dominios definidos en este documento, justificados con el código y las pruebas de este laboratorio. No es una clasificación de un fabricante.

| Dominio | Nivel | Por qué no es más alto |
|---|---:|---|
| Telemetría de aplicación | 3 | El agente, los spans de negocio y los logs JSON están en el código y las pruebas unitarias pasan. El pipeline completo no quedó observado en esta sesión: Docker Hub respondió 504 al pedir las imágenes base. |
| Correlación | 3 | Derived fields, `tracesToLogsV2` y el script de enriquecimiento están escritos. Falta ver en Grafana el salto con un `trace_id` de una petición real de este laboratorio. |
| Detección | 2 | Las reglas de Prometheus existen. No hay una alerta firing registrada. |
| SRE | 2 | El SLO, el presupuesto y la fórmula están en código y en `ErrorBudgetTest`. No hay una ventana experimental medida. |
| Seguridad y red | 2 | El 401 y el contador de autenticación son reales. Flow logs y los centros de seguridad cloud están preparados, no desplegados. No hay CVE. |
| AIOps | 2 | La regla `baseline + 2σ` más p99 está versionada. No hubo una corrida que cuente alertas estáticas frente a correlacionadas. |
| Caos y recuperación | 2 | La política está probada: desarmado no inyecta, la latencia es 200 ms, el error exige cabecera. No se midió el MTTD contra Prometheus. |
| Plataforma multicloud | 2 | Terraform, manifiestos y perfiles `gcp`/`aws` están en el repositorio. No hubo `apply`. |

## Roadmap de 90 días

| Quincena | Acción | Prioridad |
|---|---|---|
| 1 | Levantar el Compose cuando el registro de imágenes responda y guardar una traza con los tres servicios | Alta |
| 2 | Ejecutar `chaos/run_scenarios.py` y archivar MTTD, SLO y conteo de alertas | Alta |
| 3 | Separar los tres servicios ECS y activar Service Connect en un namespace de laboratorio | Media |
| 4 | Activar Cloud Service Mesh solo después de tener el fleet, y comparar sus métricas L7 con las del agente | Media |
| 5 | Encender VPC Flow Logs en una cuenta de laboratorio y un panel con rechazos reales | Media |
| 6 | Revisar si el SLO de 150 ms sigue teniendo sentido con el p99 medido, y alargar el baseline si la desviación oscila | Alta |

El éxito de la quincena 2 es un JSON en `evidence/chaos/` con `mttd_seconds` numérico, no una estimación.
