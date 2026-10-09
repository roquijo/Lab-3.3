# Observabilidad L7 en GKE

Cloud Service Mesh es el mesh gestionado de Google, basado en Istio. No está en el `kustomization.yaml`: inyectar sidecars sin tener el fleet y la feature activados deja los pods sin arrancar.

Cuando el proyecto tenga la API y el fleet, el namespace lleva la etiqueta de revisión que indique la consola (`istio.io/rev`) y los Deployments no necesitan un Envoy propio. Las métricas L7 salen del proxy hacia Cloud Monitoring. Hasta entonces, la telemetría entre servicios es la del agente OpenTelemetry.

El manifiesto de GKE de este repositorio usa MySQL en el clúster con el perfil Spring `local`. Eso no es Cloud SQL. Cloud SQL solo se usa con `create_cloud_sql=true` y `SPRING_PROFILES_ACTIVE=gcp` más `CLOUD_SQL_JDBC_URL`.
