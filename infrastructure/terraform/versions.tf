# =============================================================================
# Servantin GCP Infrastructure - Version Constraints
# =============================================================================
# This file contains version constraints for Terraform and providers.
# Separating versions is a best practice for maintainability.
# =============================================================================

terraform {
  required_version = ">= 1.5.0, < 2.0.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0, < 6.0"
    }

    google-beta = {
      source  = "hashicorp/google-beta"
      version = ">= 5.0, < 6.0"
    }

    random = {
      source  = "hashicorp/random"
      version = ">= 3.5, < 4.0"
    }
  }
}
