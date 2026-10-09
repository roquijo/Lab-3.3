# Observabilidad L7 en ECS

AWS App Mesh dejó de aceptar clientes nuevos el 24 de septiembre de 2024 y el soporte terminó el 30 de septiembre de 2026. No forma parte de esta solución.

La alternativa vigente es Amazon ECS Service Connect: un proxy Envoy por tarea, nombres cortos dentro de un namespace y métricas en CloudWatch. No está activado en `terraform/aws/ecs.tf` porque la tarea de laboratorio pone los tres servicios en la misma red `awsvpc` y se hablan por `127.0.0.1`. Ese tráfico no atraviesa un proxy. Separar cada servicio en su propia service de ECS, con Service Connect, es el paso de producción y tiene costo adicional de tareas. No se aplicó.

En local, el tráfico este-oeste se observa con las métricas `http_client_request_duration_seconds_*` del agente Java, no con un service mesh.
