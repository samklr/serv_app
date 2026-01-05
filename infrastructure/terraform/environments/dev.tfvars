# =============================================================================
# Servantin - Development Environment Configuration
# =============================================================================
# Usage: terraform apply -var-file=environments/dev.tfvars
# =============================================================================

# -----------------------------------------------------------------------------
# Project Configuration
# -----------------------------------------------------------------------------
project_id  = "your-gcp-project-id" # TODO: Replace with your project ID
region      = "europe-west1"
environment = "dev"
team_name   = "platform"

# -----------------------------------------------------------------------------
# Networking
# -----------------------------------------------------------------------------
subnet_cidr        = "10.0.0.0/24"
vpc_connector_cidr = "10.8.0.0/28"

# -----------------------------------------------------------------------------
# Database - Minimal for development
# -----------------------------------------------------------------------------
db_tier      = "db-f1-micro"
db_disk_size = 10

# -----------------------------------------------------------------------------
# Backend (API) - Minimal resources for development
# -----------------------------------------------------------------------------
backend_image_tag     = "latest"
backend_cpu           = "1"
backend_memory        = "512Mi"
backend_min_instances = 0 # Scale to zero when idle
backend_max_instances = 2

# -----------------------------------------------------------------------------
# Frontend - Minimal resources for development
# -----------------------------------------------------------------------------
frontend_image_tag     = "latest"
frontend_cpu           = "1"
frontend_memory        = "256Mi"
frontend_min_instances = 0 # Scale to zero when idle
frontend_max_instances = 2

# -----------------------------------------------------------------------------
# External Services
# -----------------------------------------------------------------------------
# Use Stripe test key for development
stripe_api_key = "sk_test_your_test_key_here"
