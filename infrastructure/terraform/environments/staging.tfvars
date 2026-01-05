# =============================================================================
# Servantin - Staging Environment Configuration
# =============================================================================
# Usage: terraform apply -var-file=environments/staging.tfvars
#
# Staging mirrors production but with reduced resources for cost savings.
# Use this environment for pre-release testing.
# =============================================================================

# -----------------------------------------------------------------------------
# Project Configuration
# -----------------------------------------------------------------------------
project_id  = "samk-303200" # TODO: Replace with staging project ID
region      = "europe-west6"
environment = "staging"
team_name   = "platform"

# -----------------------------------------------------------------------------
# Networking
# -----------------------------------------------------------------------------
subnet_cidr        = "10.0.0.0/24"
vpc_connector_cidr = "10.8.0.0/28"

# -----------------------------------------------------------------------------
# Database - Smaller than production but with same features
# -----------------------------------------------------------------------------
db_tier      = "db-g1-small"
db_disk_size = 20

# -----------------------------------------------------------------------------
# Backend (API) - Moderate resources
# -----------------------------------------------------------------------------
backend_image_tag     = "latest"
backend_cpu           = "1"
backend_memory        = "512Mi"
backend_min_instances = 0 # Scale to zero when idle (cost savings)
backend_max_instances = 5

# -----------------------------------------------------------------------------
# Frontend - Moderate resources
# -----------------------------------------------------------------------------
frontend_image_tag     = "latest"
frontend_cpu           = "1"
frontend_memory        = "512Mi"
frontend_min_instances = 0 # Scale to zero when idle (cost savings)
frontend_max_instances = 5

# -----------------------------------------------------------------------------
# External Services
# -----------------------------------------------------------------------------
# Use Stripe test key for staging
stripe_api_key = "sk_test_your_test_key_here"
