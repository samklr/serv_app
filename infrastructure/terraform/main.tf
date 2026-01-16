# =============================================================================
# Servantin GCP Infrastructure - Main Configuration
# =============================================================================
# This configuration deploys a production-ready infrastructure on GCP:
# - Cloud Run services for frontend and backend
# - Cloud SQL PostgreSQL with private networking
# - Secret Manager for sensitive configuration
# - Artifact Registry for container images
# - Cloud Storage for user uploads
# =============================================================================

# -----------------------------------------------------------------------------
# Providers
# -----------------------------------------------------------------------------
provider "google" {
  project = var.project_id
  region  = var.region

  default_labels = local.common_labels
}

provider "google-beta" {
  project = var.project_id
  region  = var.region

  default_labels = local.common_labels
}

# -----------------------------------------------------------------------------
# Local Values
# -----------------------------------------------------------------------------
locals {
  # Naming convention: {project}-{component}-{environment}
  name_prefix = "servantin-${var.environment}"

  # Common labels for all resources (cost tracking, organization)
  common_labels = {
    project     = "servantin"
    environment = var.environment
    managed_by  = "terraform"
    team        = var.team_name
  }

  # Environment-specific configurations
  is_production = var.environment == "prod"

  # Storage bucket name (to break dependency cycles)
  uploads_bucket_name = "${var.project_id}-${local.name_prefix}-uploads"

  # Database connection string for Cloud SQL
  database_connection_string = "jdbc:postgresql:///${google_sql_database.main.name}?cloudSqlInstance=${google_sql_database_instance.main.connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"

  # Artifact Registry image URLs
  backend_image_url  = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}/backend:${var.backend_image_tag}"
  frontend_image_url = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}/frontend:${var.frontend_image_tag}"

  # Required GCP APIs
  required_apis = [
    "run.googleapis.com",
    "sqladmin.googleapis.com",
    "secretmanager.googleapis.com",
    "cloudbuild.googleapis.com",
    "artifactregistry.googleapis.com",
    "vpcaccess.googleapis.com",
    "servicenetworking.googleapis.com",
    "compute.googleapis.com",
    "logging.googleapis.com",
    "monitoring.googleapis.com",
  ]
}

# -----------------------------------------------------------------------------
# Data Sources
# -----------------------------------------------------------------------------
data "google_project" "current" {
  project_id = var.project_id
}

# -----------------------------------------------------------------------------
# Enable Required APIs
# -----------------------------------------------------------------------------
resource "google_project_service" "apis" {
  for_each = toset(local.required_apis)

  project                    = var.project_id
  service                    = each.value
  disable_on_destroy         = false
  disable_dependent_services = false

  timeouts {
    create = "10m"
    update = "10m"
  }
}

# =============================================================================
# NETWORKING
# =============================================================================

# -----------------------------------------------------------------------------
# VPC Network
# -----------------------------------------------------------------------------
resource "google_compute_network" "main" {
  name                            = "${local.name_prefix}-vpc"
  auto_create_subnetworks         = false
  delete_default_routes_on_create = false
  mtu                             = 1460

  depends_on = [google_project_service.apis["compute.googleapis.com"]]
}

resource "google_compute_subnetwork" "main" {
  name                     = "${local.name_prefix}-subnet"
  ip_cidr_range            = var.subnet_cidr
  region                   = var.region
  network                  = google_compute_network.main.id
  private_ip_google_access = true

  log_config {
    aggregation_interval = "INTERVAL_5_SEC"
    flow_sampling        = 0.5
    metadata             = "INCLUDE_ALL_METADATA"
  }
}

# -----------------------------------------------------------------------------
# Private Service Access (for Cloud SQL)
# -----------------------------------------------------------------------------
resource "google_compute_global_address" "private_ip_range" {
  name          = "${local.name_prefix}-private-ip"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.main.id
}

resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.main.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_range.name]

  depends_on = [google_project_service.apis["servicenetworking.googleapis.com"]]
}

# -----------------------------------------------------------------------------
# VPC Access Connector (for Cloud Run -> Cloud SQL)
# -----------------------------------------------------------------------------
resource "google_vpc_access_connector" "main" {
  name          = "serv-${var.environment}-conn"
  region        = var.region
  ip_cidr_range = var.vpc_connector_cidr
  network       = google_compute_network.main.name

  min_instances = local.is_production ? 2 : 2
  max_instances = local.is_production ? 10 : 3

  machine_type = local.is_production ? "e2-standard-4" : "e2-micro"

  depends_on = [google_project_service.apis["vpcaccess.googleapis.com"]]
}

# =============================================================================
# ARTIFACT REGISTRY
# =============================================================================
resource "google_artifact_registry_repository" "main" {
  location      = var.region
  repository_id = local.name_prefix
  description   = "Docker repository for Servantin ${var.environment} environment"
  format        = "DOCKER"

  cleanup_policies {
    id     = "keep-minimum-versions"
    action = "KEEP"

    most_recent_versions {
      keep_count = local.is_production ? 10 : 5
    }
  }

  cleanup_policies {
    id     = "delete-old-untagged"
    action = "DELETE"

    condition {
      tag_state  = "UNTAGGED"
      older_than = local.is_production ? "2592000s" : "604800s" # 30 days / 7 days
    }
  }

  depends_on = [google_project_service.apis["artifactregistry.googleapis.com"]]
}

# =============================================================================
# DATABASE (Cloud SQL)
# =============================================================================

# -----------------------------------------------------------------------------
# Random suffix for database instance name (immutable)
# -----------------------------------------------------------------------------
resource "random_id" "db_suffix" {
  byte_length = 4

  keepers = {
    # Regenerate only if project or environment changes
    project     = var.project_id
    environment = var.environment
  }
}

# -----------------------------------------------------------------------------
# Cloud SQL PostgreSQL Instance
# -----------------------------------------------------------------------------
resource "google_sql_database_instance" "main" {
  name             = "${local.name_prefix}-db-${random_id.db_suffix.hex}"
  database_version = "POSTGRES_16"
  region           = var.region

  deletion_protection = local.is_production

  settings {
    tier              = var.db_tier
    availability_type = local.is_production ? "REGIONAL" : "ZONAL"
    disk_size         = var.db_disk_size
    disk_type         = "PD_SSD"
    disk_autoresize   = true

    # Limit disk growth
    disk_autoresize_limit = local.is_production ? 500 : 50

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      location                       = var.region
      point_in_time_recovery_enabled = local.is_production

      backup_retention_settings {
        retained_backups = local.is_production ? 30 : 7
        retention_unit   = "COUNT"
      }
    }

    ip_configuration {
      ipv4_enabled                                  = false
      private_network                               = google_compute_network.main.id
      enable_private_path_for_google_cloud_services = true
      ssl_mode                                      = local.is_production ? "ENCRYPTED_ONLY" : "ALLOW_UNENCRYPTED_AND_ENCRYPTED"
    }

    maintenance_window {
      day          = 7 # Sunday
      hour         = 4 # 4 AM
      update_track = local.is_production ? "stable" : "canary"
    }

    insights_config {
      query_insights_enabled  = true
      query_plans_per_minute  = 5
      query_string_length     = 1024
      record_application_tags = true
      record_client_address   = false
    }

    database_flags {
      name  = "max_connections"
      value = local.is_production ? "200" : "100"
    }

    database_flags {
      name  = "log_checkpoints"
      value = "on"
    }

    database_flags {
      name  = "log_connections"
      value = "on"
    }

    database_flags {
      name  = "log_disconnections"
      value = "on"
    }

    user_labels = local.common_labels
  }

  depends_on = [google_service_networking_connection.private_vpc_connection]

  lifecycle {
    prevent_destroy = false # Set to true for production
    ignore_changes = [
      settings[0].disk_size, # Allow disk autoresize
    ]
  }

  timeouts {
    create = "30m"
    update = "30m"
    delete = "30m"
  }
}

# -----------------------------------------------------------------------------
# Database
# -----------------------------------------------------------------------------
resource "google_sql_database" "main" {
  name     = "servantin"
  instance = google_sql_database_instance.main.name

  deletion_policy = local.is_production ? "ABANDON" : "DELETE"
}

# -----------------------------------------------------------------------------
# Database User
# -----------------------------------------------------------------------------
resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"

  keepers = {
    instance = google_sql_database_instance.main.name
  }
}

resource "google_sql_user" "main" {
  name     = "servantin"
  instance = google_sql_database_instance.main.name
  password = random_password.db_password.result

  deletion_policy = "ABANDON"
}

# =============================================================================
# SECRETS MANAGEMENT
# =============================================================================

# -----------------------------------------------------------------------------
# Database Password Secret
# -----------------------------------------------------------------------------
resource "google_secret_manager_secret" "db_password" {
  secret_id = "${local.name_prefix}-db-password"

  labels = local.common_labels

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db_password.result

  lifecycle {
    create_before_destroy = true
  }
}

# -----------------------------------------------------------------------------
# JWT Secret
# -----------------------------------------------------------------------------
resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "${local.name_prefix}-jwt-secret"

  labels = local.common_labels

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret_version" "jwt_secret" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = random_password.jwt_secret.result

  lifecycle {
    create_before_destroy = true
  }
}

# -----------------------------------------------------------------------------
# Stripe API Key Secret
# -----------------------------------------------------------------------------
resource "google_secret_manager_secret" "stripe_api_key" {
  secret_id = "${local.name_prefix}-stripe-api-key"

  labels = local.common_labels

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret_version" "stripe_api_key" {
  secret      = google_secret_manager_secret.stripe_api_key.id
  secret_data = var.stripe_api_key

  lifecycle {
    create_before_destroy = true
  }
}

# -----------------------------------------------------------------------------
# AWS SES Secrets (for email notifications)
# -----------------------------------------------------------------------------
resource "google_secret_manager_secret" "aws_access_key_id" {
  secret_id = "${local.name_prefix}-aws-access-key-id"

  labels = local.common_labels

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret_version" "aws_access_key_id" {
  secret      = google_secret_manager_secret.aws_access_key_id.id
  secret_data = var.aws_access_key_id

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_secret_manager_secret" "aws_secret_access_key" {
  secret_id = "${local.name_prefix}-aws-secret-access-key"

  labels = local.common_labels

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret_version" "aws_secret_access_key" {
  secret      = google_secret_manager_secret.aws_secret_access_key.id
  secret_data = var.aws_secret_access_key

  lifecycle {
    create_before_destroy = true
  }
}

# =============================================================================
# IAM & SERVICE ACCOUNTS
# =============================================================================

# -----------------------------------------------------------------------------
# Cloud Run Service Account
# -----------------------------------------------------------------------------
resource "google_service_account" "cloud_run" {
  account_id   = "${local.name_prefix}-run"
  display_name = "Servantin Cloud Run Service Account (${var.environment})"
  description  = "Service account for Cloud Run services in ${var.environment} environment"
}

# -----------------------------------------------------------------------------
# IAM Bindings
# -----------------------------------------------------------------------------

# Cloud SQL access
resource "google_project_iam_member" "cloud_run_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloud_run.email}"
}

# Secret Manager access (individual secrets)
resource "google_secret_manager_secret_iam_member" "db_password_access" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_secret_manager_secret_iam_member" "jwt_secret_access" {
  secret_id = google_secret_manager_secret.jwt_secret.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_secret_manager_secret_iam_member" "stripe_key_access" {
  secret_id = google_secret_manager_secret.stripe_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

# Cloud Storage access
resource "google_project_iam_member" "cloud_run_storage" {
  project = var.project_id
  role    = "roles/storage.objectUser"
  member  = "serviceAccount:${google_service_account.cloud_run.email}"
}

# Logging access
resource "google_project_iam_member" "cloud_run_logging" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.cloud_run.email}"
}

# Monitoring access
resource "google_project_iam_member" "cloud_run_monitoring" {
  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.cloud_run.email}"
}

# =============================================================================
# CLOUD RUN SERVICES
# =============================================================================

# -----------------------------------------------------------------------------
# Backend Service (Spring Boot API)
# -----------------------------------------------------------------------------
resource "google_cloud_run_v2_service" "backend" {
  name     = "${local.name_prefix}-api"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  labels = local.common_labels

  template {
    labels = merge(local.common_labels, {
      component = "backend"
    })

    service_account = google_service_account.cloud_run.email
    timeout         = "300s"

    execution_environment = "EXECUTION_ENVIRONMENT_GEN2"

    scaling {
      min_instance_count = local.is_production ? var.backend_min_instances : 0
      max_instance_count = var.backend_max_instances
    }

    vpc_access {
      connector = google_vpc_access_connector.main.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    containers {
      name  = "backend"
      image = local.backend_image_url

      resources {
        limits = {
          cpu    = var.backend_cpu
          memory = var.backend_memory
        }
        cpu_idle          = !local.is_production
        startup_cpu_boost = true
      }

      ports {
        name           = "http1"
        container_port = 8080
      }

      # Application Configuration
      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }

      env {
        name  = "DATABASE_URL"
        value = local.database_connection_string
      }

      env {
        name  = "DATABASE_USER"
        value = google_sql_user.main.name
      }

      env {
        name = "DATABASE_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.jwt_secret.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "STRIPE_API_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.stripe_api_key.secret_id
            version = "latest"
          }
        }
      }

      env {
        name  = "STORAGE_BUCKET"
        value = local.uploads_bucket_name
      }

      # Health Probes
      startup_probe {
        http_get {
          path = "/actuator/health/readiness"
          port = 8080
        }
        initial_delay_seconds = 10
        timeout_seconds       = 5
        period_seconds        = 10
        failure_threshold     = 30
      }

      liveness_probe {
        http_get {
          path = "/actuator/health/liveness"
          port = 8080
        }
        initial_delay_seconds = 0
        timeout_seconds       = 5
        period_seconds        = 30
        failure_threshold     = 3
      }
    }

    # Cloud SQL sidecar connection
    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = [google_sql_database_instance.main.connection_name]
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image, # Allow CI/CD to update image
      client,
      client_version,
    ]
  }

  depends_on = [
    google_project_service.apis,
    google_secret_manager_secret_version.db_password,
    google_secret_manager_secret_version.jwt_secret,
    google_secret_manager_secret_iam_member.db_password_access,
    google_secret_manager_secret_iam_member.jwt_secret_access,
    google_project_iam_member.cloud_run_sql_client,
  ]
}

# -----------------------------------------------------------------------------
# Frontend Service (Next.js)
# -----------------------------------------------------------------------------
resource "google_cloud_run_v2_service" "frontend" {
  name     = "${local.name_prefix}-web"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  labels = local.common_labels

  template {
    labels = merge(local.common_labels, {
      component = "frontend"
    })

    service_account = google_service_account.cloud_run.email
    timeout         = "60s"

    execution_environment = "EXECUTION_ENVIRONMENT_GEN2"

    scaling {
      min_instance_count = local.is_production ? var.frontend_min_instances : 0
      max_instance_count = var.frontend_max_instances
    }

    containers {
      name  = "frontend"
      image = local.frontend_image_url

      resources {
        limits = {
          cpu    = var.frontend_cpu
          memory = var.frontend_memory
        }
        cpu_idle          = !local.is_production
        startup_cpu_boost = true
      }

      ports {
        name           = "http1"
        container_port = 3000
      }

      env {
        name  = "NODE_ENV"
        value = "production"
      }

      env {
        name  = "NEXT_PUBLIC_API_URL"
        value = ""
      }

      env {
        name  = "BACKEND_INTERNAL_URL"
        value = google_cloud_run_v2_service.backend.uri
      }

      startup_probe {
        http_get {
          path = "/"
          port = 3000
        }
        initial_delay_seconds = 0
        timeout_seconds       = 3
        period_seconds        = 5
        failure_threshold     = 20
      }

      liveness_probe {
        http_get {
          path = "/"
          port = 3000
        }
        initial_delay_seconds = 0
        timeout_seconds       = 3
        period_seconds        = 30
        failure_threshold     = 3
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image, # Allow CI/CD to update image
      client,
      client_version,
    ]
  }

  depends_on = [google_project_service.apis]
}

# -----------------------------------------------------------------------------
# Public Access for Cloud Run Services
# -----------------------------------------------------------------------------
resource "google_cloud_run_v2_service_iam_member" "backend_public" {
  location = google_cloud_run_v2_service.backend.location
  name     = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

resource "google_cloud_run_v2_service_iam_member" "frontend_public" {
  location = google_cloud_run_v2_service.frontend.location
  name     = google_cloud_run_v2_service.frontend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# =============================================================================
# CLOUD STORAGE
# =============================================================================
resource "google_storage_bucket" "uploads" {
  name          = local.uploads_bucket_name
  location      = var.region
  storage_class = "STANDARD"

  # Only allow force destroy in non-production
  force_destroy = !local.is_production

  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  labels = local.common_labels

  versioning {
    enabled = local.is_production
  }

  cors {
    origin          = local.is_production ? [google_cloud_run_v2_service.frontend.uri] : ["*"]
    method          = ["GET", "PUT", "POST", "DELETE"]
    response_header = ["Content-Type", "Content-Disposition"]
    max_age_seconds = 3600
  }

  # Lifecycle rules for cost optimization
  lifecycle_rule {
    condition {
      age                   = 365
      matches_storage_class = ["STANDARD"]
    }
    action {
      type          = "SetStorageClass"
      storage_class = "NEARLINE"
    }
  }

  lifecycle_rule {
    condition {
      age = 730 # 2 years
    }
    action {
      type = "Delete"
    }
  }

  # Delete incomplete multipart uploads
  lifecycle_rule {
    condition {
      age                        = 7
      with_state                 = "ANY"
      num_newer_versions         = 0
      matches_prefix             = []
      matches_suffix             = []
      matches_storage_class      = []
      days_since_custom_time     = 0
      days_since_noncurrent_time = 0
      noncurrent_time_before     = ""
      custom_time_before         = ""
      created_before             = ""
    }
    action {
      type = "AbortIncompleteMultipartUpload"
    }
  }

  depends_on = [google_project_service.apis["storage.googleapis.com"]]
}

resource "google_storage_bucket_iam_member" "uploads_access" {
  bucket = google_storage_bucket.uploads.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.cloud_run.email}"
}
