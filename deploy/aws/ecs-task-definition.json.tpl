[
  {
    "name": "otel-collector",
    "image": "${collector_image}",
    "essential": true,
    "cpu": 256,
    "memory": 512,
    "portMappings": [
      { "containerPort": 4317, "protocol": "tcp", "name": "otlp-grpc" },
      { "containerPort": 4318, "protocol": "tcp", "name": "otlp-http" },
      { "containerPort": 13133, "protocol": "tcp", "name": "health" }
    ],
    "environment": [
      { "name": "AWS_REGION", "value": "${region}" },
      { "name": "DEPLOYMENT_ENVIRONMENT", "value": "aws" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/lab33/collector",
        "awslogs-region": "${region}",
        "awslogs-stream-prefix": "collector"
      }
    },
    "healthCheck": {
      "command": ["CMD", "/otelcol-contrib", "--version"],
      "interval": 30,
      "timeout": 5,
      "retries": 3,
      "startPeriod": 10
    }
  },
%{ if include_mysql == "true" }
  {
    "name": "mysql",
    "image": "mysql:8.4",
    "essential": false,
    "cpu": 256,
    "memory": 512,
    "command": ["--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci"],
    "environment": [
      { "name": "MYSQL_DATABASE", "value": "lab33" },
      { "name": "MYSQL_USER", "value": "lab" }
    ],
    "secrets": [
      { "name": "MYSQL_PASSWORD", "valueFrom": "${db_password_arn}" },
      { "name": "MYSQL_ROOT_PASSWORD", "valueFrom": "${db_password_arn}" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/lab33/mysql",
        "awslogs-region": "${region}",
        "awslogs-stream-prefix": "mysql"
      }
    }
  },
%{ endif }
  {
    "name": "data-service",
    "image": "${data_service_image}",
    "essential": true,
    "cpu": 256,
    "memory": 768,
    "portMappings": [
      { "containerPort": 8082, "protocol": "tcp", "name": "data-http" }
    ],
    "environment": [
      { "name": "SERVER_PORT", "value": "8082" },
      { "name": "OTEL_ENABLED", "value": "true" },
      { "name": "OTEL_SERVICE_NAME", "value": "data-service" },
      { "name": "OTEL_EXPORTER_OTLP_ENDPOINT", "value": "http://127.0.0.1:4317" },
      { "name": "OTEL_EXPORTER_OTLP_PROTOCOL", "value": "grpc" },
      { "name": "OTEL_TRACES_EXPORTER", "value": "otlp" },
      { "name": "OTEL_METRICS_EXPORTER", "value": "otlp" },
      { "name": "OTEL_LOGS_EXPORTER", "value": "otlp" },
      { "name": "OTEL_TRACES_SAMPLER", "value": "parentbased_traceidratio" },
      { "name": "OTEL_TRACES_SAMPLER_ARG", "value": "1.0" },
      { "name": "OTEL_PROPAGATORS", "value": "tracecontext,baggage" },
      { "name": "OTEL_JAVAAGENT_EXCLUDE_URLS", "value": ".*/actuator/.*,.*/api/chaos.*" },
      { "name": "OTEL_RESOURCE_ATTRIBUTES", "value": "service.version=1.0.0,deployment.environment=aws,service.namespace=lab33,cloud.provider=aws" },
      { "name": "DEPLOYMENT_ENVIRONMENT", "value": "aws" },
      { "name": "SPRING_PROFILES_ACTIVE", "value": "aws" },
      { "name": "RDS_JDBC_URL", "value": "${jdbc_url}" },
      { "name": "SPRING_DATASOURCE_USERNAME", "value": "lab" },
      { "name": "CHAOS_ARMED", "value": "false" }
    ],
    "secrets": [
      { "name": "SPRING_DATASOURCE_PASSWORD", "valueFrom": "${db_password_arn}" }
    ],
    "dependsOn": [
      { "containerName": "otel-collector", "condition": "START" }%{ if include_mysql == "true" },
      { "containerName": "mysql", "condition": "START" }%{ endif }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/lab33/data-service",
        "awslogs-region": "${region}",
        "awslogs-stream-prefix": "data-service"
      }
    }
  },
  {
    "name": "service-b",
    "image": "${service_b_image}",
    "essential": true,
    "cpu": 256,
    "memory": 768,
    "portMappings": [
      { "containerPort": 8081, "protocol": "tcp", "name": "http" }
    ],
    "environment": [
      { "name": "SERVER_PORT", "value": "8081" },
      { "name": "OTEL_ENABLED", "value": "true" },
      { "name": "OTEL_SERVICE_NAME", "value": "service-b" },
      { "name": "OTEL_EXPORTER_OTLP_ENDPOINT", "value": "http://127.0.0.1:4317" },
      { "name": "OTEL_EXPORTER_OTLP_PROTOCOL", "value": "grpc" },
      { "name": "OTEL_TRACES_EXPORTER", "value": "otlp" },
      { "name": "OTEL_METRICS_EXPORTER", "value": "otlp" },
      { "name": "OTEL_LOGS_EXPORTER", "value": "otlp" },
      { "name": "OTEL_TRACES_SAMPLER", "value": "parentbased_traceidratio" },
      { "name": "OTEL_TRACES_SAMPLER_ARG", "value": "1.0" },
      { "name": "OTEL_PROPAGATORS", "value": "tracecontext,baggage" },
      { "name": "OTEL_JAVAAGENT_EXCLUDE_URLS", "value": ".*/actuator/.*" },
      { "name": "OTEL_RESOURCE_ATTRIBUTES", "value": "service.version=1.0.0,deployment.environment=aws,service.namespace=lab33,cloud.provider=aws" },
      { "name": "DEPLOYMENT_ENVIRONMENT", "value": "aws" },
      { "name": "DATA_SERVICE_URL", "value": "http://127.0.0.1:8082" },
      { "name": "CHAOS_ARMED", "value": "false" },
      { "name": "LAB_API_TOKEN", "value": "${lab_api_token}" }
    ],
    "dependsOn": [
      { "containerName": "otel-collector", "condition": "START" },
      { "containerName": "data-service", "condition": "START" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/lab33/service-b",
        "awslogs-region": "${region}",
        "awslogs-stream-prefix": "service-b"
      }
    }
  },
  {
    "name": "service-a",
    "image": "${service_a_image}",
    "essential": true,
    "cpu": 256,
    "memory": 768,
    "portMappings": [
      { "containerPort": 8080, "protocol": "tcp", "name": "http" }
    ],
    "environment": [
      { "name": "SERVER_PORT", "value": "8080" },
      { "name": "OTEL_ENABLED", "value": "true" },
      { "name": "OTEL_SERVICE_NAME", "value": "service-a" },
      { "name": "OTEL_EXPORTER_OTLP_ENDPOINT", "value": "http://127.0.0.1:4317" },
      { "name": "OTEL_EXPORTER_OTLP_PROTOCOL", "value": "grpc" },
      { "name": "OTEL_TRACES_EXPORTER", "value": "otlp" },
      { "name": "OTEL_METRICS_EXPORTER", "value": "otlp" },
      { "name": "OTEL_LOGS_EXPORTER", "value": "otlp" },
      { "name": "OTEL_TRACES_SAMPLER", "value": "parentbased_traceidratio" },
      { "name": "OTEL_TRACES_SAMPLER_ARG", "value": "1.0" },
      { "name": "OTEL_PROPAGATORS", "value": "tracecontext,baggage" },
      { "name": "OTEL_JAVAAGENT_EXCLUDE_URLS", "value": ".*/actuator/.*" },
      { "name": "OTEL_RESOURCE_ATTRIBUTES", "value": "service.version=1.0.0,deployment.environment=aws,service.namespace=lab33,cloud.provider=aws" },
      { "name": "DEPLOYMENT_ENVIRONMENT", "value": "aws" },
      { "name": "SERVICE_B_URL", "value": "http://127.0.0.1:8081" },
      { "name": "CHAOS_ARMED", "value": "false" },
      { "name": "LAB_API_TOKEN", "value": "${lab_api_token}" }
    ],
    "dependsOn": [
      { "containerName": "otel-collector", "condition": "START" },
      { "containerName": "service-b", "condition": "START" }%{ if include_mysql == "true" },
      { "containerName": "mysql", "condition": "START" }%{ endif }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/lab33/service-a",
        "awslogs-region": "${region}",
        "awslogs-stream-prefix": "service-a"
      }
    }
  }
]
