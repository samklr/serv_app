# =============================================================================
# Servantin GCP Infrastructure - Variables
# =============================================================================

# -----------------------------------------------------------------------------
# Project Configuration
# -----------------------------------------------------------------------------
variable "project_id" {
  description = "GCP Project ID where resources will be created"
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{4,28}[a-z0-9]$", var.project_id))
    error_message = "Project ID must be 6-30 characters, start with a letter, and contain only lowercase letters, numbers, and hyphens."
  }
}

variable "region" {
  description = "GCP region for resources"
  type        = string
  default     = "europe-west1"

  validation {
    condition = contains([
      "us-central1", "us-east1", "us-east4", "us-west1",
      "europe-west1", "europe-west2", "europe-west3", "europe-west4", "europe-west6", "europe-west9",
      "asia-east1", "asia-northeast1", "asia-southeast1"
    ], var.region)
    error_message = "Region must be a valid GCP region with Cloud Run support."
  }
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod."
  }
}

variable "team_name" {
  description = "Team name for resource labeling and cost tracking"
  type        = string
  default     = "platform"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{0,62}$", var.team_name))
    error_message = "Team name must be lowercase alphanumeric with hyphens, max 63 characters."
  }
}

# -----------------------------------------------------------------------------
# Networking Configuration
# -----------------------------------------------------------------------------
variable "subnet_cidr" {
  description = "CIDR range for the main subnet"
  type        = string
  default     = "10.0.0.0/24"

  validation {
    condition     = can(cidrhost(var.subnet_cidr, 0))
    error_message = "Subnet CIDR must be a valid CIDR notation."
  }
}

variable "vpc_connector_cidr" {
  description = "CIDR range for VPC Access Connector (must be /28)"
  type        = string
  default     = "10.8.0.0/28"

  validation {
    condition     = can(regex("/28$", var.vpc_connector_cidr))
    error_message = "VPC Connector CIDR must end with /28."
  }
}

# -----------------------------------------------------------------------------
# Database Configuration
# -----------------------------------------------------------------------------
variable "db_tier" {
  description = "Cloud SQL instance tier"
  type        = string
  default     = "db-f1-micro"

  validation {
    condition     = can(regex("^db-(f1-micro|g1-small|custom-[0-9]+-[0-9]+|n1-(standard|highmem|highcpu)-[0-9]+)$", var.db_tier))
    error_message = "DB tier must be a valid Cloud SQL tier (e.g., db-f1-micro, db-custom-2-4096)."
  }
}

variable "db_disk_size" {
  description = "Cloud SQL disk size in GB"
  type        = number
  default     = 10

  validation {
    condition     = var.db_disk_size >= 10 && var.db_disk_size <= 65536
    error_message = "DB disk size must be between 10 and 65536 GB."
  }
}

# -----------------------------------------------------------------------------
# Backend (API) Configuration
# -----------------------------------------------------------------------------
variable "backend_image_tag" {
  description = "Docker image tag for backend service"
  type        = string
  default     = "latest"
}

variable "backend_cpu" {
  description = "CPU allocation for backend (e.g., '1', '2', '4')"
  type        = string
  default     = "1"

  validation {
    condition     = contains(["1", "2", "4", "6", "8"], var.backend_cpu)
    error_message = "Backend CPU must be 1, 2, 4, 6, or 8."
  }
}

variable "backend_memory" {
  description = "Memory allocation for backend (e.g., '512Mi', '1Gi', '2Gi')"
  type        = string
  default     = "512Mi"

  validation {
    condition     = can(regex("^[0-9]+(Mi|Gi)$", var.backend_memory))
    error_message = "Backend memory must be specified in Mi or Gi (e.g., 512Mi, 1Gi)."
  }
}

variable "backend_min_instances" {
  description = "Minimum number of backend instances (for prod)"
  type        = number
  default     = 1

  validation {
    condition     = var.backend_min_instances >= 0 && var.backend_min_instances <= 100
    error_message = "Backend min instances must be between 0 and 100."
  }
}

variable "backend_max_instances" {
  description = "Maximum number of backend instances"
  type        = number
  default     = 5

  validation {
    condition     = var.backend_max_instances >= 1 && var.backend_max_instances <= 1000
    error_message = "Backend max instances must be between 1 and 1000."
  }
}

# -----------------------------------------------------------------------------
# Frontend Configuration
# -----------------------------------------------------------------------------
variable "frontend_image_tag" {
  description = "Docker image tag for frontend service"
  type        = string
  default     = "latest"
}

variable "frontend_cpu" {
  description = "CPU allocation for frontend"
  type        = string
  default     = "1"

  validation {
    condition     = contains(["1", "2", "4"], var.frontend_cpu)
    error_message = "Frontend CPU must be 1, 2, or 4."
  }
}

variable "frontend_memory" {
  description = "Memory allocation for frontend"
  type        = string
  default     = "512Mi"

  validation {
    condition     = can(regex("^[0-9]+(Mi|Gi)$", var.frontend_memory))
    error_message = "Frontend memory must be specified in Mi or Gi."
  }
}

variable "frontend_min_instances" {
  description = "Minimum number of frontend instances (for prod)"
  type        = number
  default     = 1

  validation {
    condition     = var.frontend_min_instances >= 0 && var.frontend_min_instances <= 100
    error_message = "Frontend min instances must be between 0 and 100."
  }
}

variable "frontend_max_instances" {
  description = "Maximum number of frontend instances"
  type        = number
  default     = 5

  validation {
    condition     = var.frontend_max_instances >= 1 && var.frontend_max_instances <= 1000
    error_message = "Frontend max instances must be between 1 and 1000."
  }
}

# -----------------------------------------------------------------------------
# External Services
# -----------------------------------------------------------------------------
variable "stripe_api_key" {
  description = "Stripe API key (use Secret Manager in production)"
  type        = string
  sensitive   = true
  default     = "sk_test_placeholder"

  validation {
    condition     = can(regex("^sk_(test|live)_", var.stripe_api_key)) || var.stripe_api_key == "sk_test_placeholder"
    error_message = "Stripe API key must start with 'sk_test_' or 'sk_live_'."
  }
}
