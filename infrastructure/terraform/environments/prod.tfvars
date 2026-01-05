# =============================================================================
# Servantin - Production Environment Configuration
# =============================================================================
# Usage: terraform apply -var-file=environments/prod.tfvars
#
# IMPORTANT: For production, manage sensitive values via:
# - Terraform Cloud variables
# - GitHub Actions secrets
# - Environment variables with TF_VAR_ prefix
# =============================================================================

# -----------------------------------------------------------------------------
# Project Configuration
# -----------------------------------------------------------------------------
project_id  = "your-gcp-project-id-prod" # TODO: Replace with production project ID
region      = "europe-west1"
environment = "prod"
team_name   = "platform"

# -----------------------------------------------------------------------------
# Networking
# -----------------------------------------------------------------------------
subnet_cidr        = "10.0.0.0/24"
vpc_connector_cidr = "10.8.0.0/28"

# -----------------------------------------------------------------------------
# Database - Production sizing
# -----------------------------------------------------------------------------
# db-custom-2-4096 = 2 vCPU, 4GB RAM (suitable for ~100 concurrent users)
# db-custom-4-8192 = 4 vCPU, 8GB RAM (suitable for ~500 concurrent users)
db_tier      = "db-custom-2-4096"
db_disk_size = 50

# -----------------------------------------------------------------------------
# Backend (API) - Production resources
# -----------------------------------------------------------------------------
backend_image_tag     = "v1.0.0" # Use specific version tags in production
backend_cpu           = "2"
backend_memory        = "1Gi"
backend_min_instances = 1 # Keep at least 1 instance warm for low latency
backend_max_instances = 10

# -----------------------------------------------------------------------------
# Frontend - Production resources
# -----------------------------------------------------------------------------
frontend_image_tag     = "v1.0.0" # Use specific version tags in production
frontend_cpu           = "1"
frontend_memory        = "512Mi"
frontend_min_instances = 1 # Keep at least 1 instance warm for low latency
frontend_max_instances = 10

# -----------------------------------------------------------------------------
# External Services
# -----------------------------------------------------------------------------
# IMPORTANT: Do NOT commit live keys to version control!
# Set via: export TF_VAR_stripe_api_key="sk_live_..."
# Or use Terraform Cloud/GitHub Actions secrets
# stripe_api_key = "sk_live_..." # NEVER commit this!
