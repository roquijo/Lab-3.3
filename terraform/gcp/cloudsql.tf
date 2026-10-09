variable "create_cloud_sql" {
  description = "Crea Cloud SQL. El valor por defecto evita un recurso de pago."
  type        = bool
  default     = false
}

resource "google_sql_database_instance" "lab33" {
  count               = var.create_cloud_sql ? 1 : 0
  name                = "lab33-mysql"
  region              = var.region
  database_version    = "MYSQL_8_4"
  deletion_protection = true

  settings {
    tier = "db-f1-micro"
    ip_configuration {
      ipv4_enabled = false
      private_network = google_compute_network.lab33.id
    }
  }
}

resource "google_sql_database" "lab33" {
  count    = var.create_cloud_sql ? 1 : 0
  name     = "lab33"
  instance = google_sql_database_instance.lab33[0].name
}

resource "google_sql_user" "lab" {
  count    = var.create_cloud_sql ? 1 : 0
  name     = "lab"
  instance = google_sql_database_instance.lab33[0].name
  password = var.cloud_sql_password
}

variable "cloud_sql_password" {
  type      = string
  default   = null
  sensitive = true
}
