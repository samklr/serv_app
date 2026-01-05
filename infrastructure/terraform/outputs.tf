# =============================================================================
# Servantin GCP Infrastructure - Outputs
# =============================================================================

# -----------------------------------------------------------------------------
# Service URLs
# -----------------------------------------------------------------------------
output "frontend_url" {
  description = "URL of the frontend Cloud Run service"
  value       = google_cloud_run_v2_service.frontend.uri
}

output "backend_url" {
  description = "URL of the backend Cloud Run service (API)"
  value       = google_cloud_run_v2_service.backend.uri
}

output "swagger_ui_url" {
  description = "URL of the Swagger UI for API documentation"
  value       = "${google_cloud_run_v2_service.backend.uri}/swagger-ui.html"
}

# -----------------------------------------------------------------------------
# Database
# -----------------------------------------------------------------------------
output "database_instance_name" {
  description = "Cloud SQL instance name"
  value       = google_sql_database_instance.main.name
}

output "database_connection_name" {
  description = "Cloud SQL instance connection name (for Cloud Run)"
  value       = google_sql_database_instance.main.connection_name
}

output "database_private_ip" {
  description = "Cloud SQL private IP address"
  value       = google_sql_database_instance.main.private_ip_address
  sensitive   = true
}

output "database_name" {
  description = "Database name"
  value       = google_sql_database.main.name
}

output "database_user" {
  description = "Database username"
  value       = google_sql_user.main.name
}

# -----------------------------------------------------------------------------
# Artifact Registry
# -----------------------------------------------------------------------------
output "artifact_registry_url" {
  description = "Artifact Registry URL for Docker images"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}"
}

output "backend_image" {
  description = "Full backend Docker image path"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}/backend"
}

output "frontend_image" {
  description = "Full frontend Docker image path"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}/frontend"
}

# -----------------------------------------------------------------------------
# Storage
# -----------------------------------------------------------------------------
output "uploads_bucket" {
  description = "Cloud Storage bucket for user uploads"
  value       = google_storage_bucket.uploads.name
}

output "uploads_bucket_url" {
  description = "Cloud Storage bucket URL"
  value       = google_storage_bucket.uploads.url
}

# -----------------------------------------------------------------------------
# Networking
# -----------------------------------------------------------------------------
output "vpc_name" {
  description = "VPC network name"
  value       = google_compute_network.main.name
}

output "vpc_connector_name" {
  description = "VPC Access Connector name (for serverless access)"
  value       = google_vpc_access_connector.main.name
}

output "subnet_name" {
  description = "Subnet name"
  value       = google_compute_subnetwork.main.name
}

# -----------------------------------------------------------------------------
# Service Accounts
# -----------------------------------------------------------------------------
output "cloud_run_service_account" {
  description = "Cloud Run service account email"
  value       = google_service_account.cloud_run.email
}

# -----------------------------------------------------------------------------
# Secrets (names only, not values)
# -----------------------------------------------------------------------------
output "secret_db_password_name" {
  description = "Secret Manager secret name for database password"
  value       = google_secret_manager_secret.db_password.secret_id
}

output "secret_jwt_name" {
  description = "Secret Manager secret name for JWT secret"
  value       = google_secret_manager_secret.jwt_secret.secret_id
}

output "secret_stripe_name" {
  description = "Secret Manager secret name for Stripe API key"
  value       = google_secret_manager_secret.stripe_api_key.secret_id
}

# -----------------------------------------------------------------------------
# Deployment Commands
# -----------------------------------------------------------------------------
output "docker_login_command" {
  description = "Command to authenticate Docker with Artifact Registry"
  value       = "gcloud auth configure-docker ${var.region}-docker.pkg.dev"
}

output "backend_deploy_command" {
  description = "Command to deploy backend to Cloud Run"
  value       = "gcloud run deploy ${google_cloud_run_v2_service.backend.name} --image ${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}/backend:latest --region ${var.region}"
}

output "frontend_deploy_command" {
  description = "Command to deploy frontend to Cloud Run"
  value       = "gcloud run deploy ${google_cloud_run_v2_service.frontend.name} --image ${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}/frontend:latest --region ${var.region}"
}

# -----------------------------------------------------------------------------
# Summary
# -----------------------------------------------------------------------------
output "deployment_summary" {
  description = "Summary of deployed infrastructure"
  value = {
    environment = var.environment
    region      = var.region
    project_id  = var.project_id

    urls = {
      frontend   = google_cloud_run_v2_service.frontend.uri
      backend    = google_cloud_run_v2_service.backend.uri
      swagger_ui = "${google_cloud_run_v2_service.backend.uri}/swagger-ui.html"
    }

    database = {
      instance_name   = google_sql_database_instance.main.name
      connection_name = google_sql_database_instance.main.connection_name
      database_name   = google_sql_database.main.name
    }

    storage = {
      uploads_bucket = google_storage_bucket.uploads.name
      registry       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.main.repository_id}"
    }
  }
}
