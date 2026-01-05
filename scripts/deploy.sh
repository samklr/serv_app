#!/bin/bash
# Deploy script for Servantin to GCP
# Usage: ./deploy.sh [dev|staging|prod]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TERRAFORM_DIR="$ROOT_DIR/infrastructure/terraform"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Default environment
ENVIRONMENT="${1:-dev}"
TFVARS_FILE="$TERRAFORM_DIR/environments/${ENVIRONMENT}.tfvars"

if [ ! -f "$TFVARS_FILE" ]; then
    log_error "Environment file not found: $TFVARS_FILE"
    exit 1
fi

log_info "Deploying Servantin to environment: $ENVIRONMENT"

# Check required tools
for cmd in gcloud terraform docker; do
    if ! command -v $cmd &> /dev/null; then
        log_error "$cmd is required but not installed."
        exit 1
    fi
done

# Extract project and region from tfvars
PROJECT_ID=$(grep -E '^project_id[[:space:]]*=' "$TFVARS_FILE" | cut -d'"' -f2)
REGION=$(grep -E '^region[[:space:]]*=' "$TFVARS_FILE" | cut -d'"' -f2)

if [ -z "$PROJECT_ID" ] || [[ "$PROJECT_ID" == *"your-gcp-project-id"* ]]; then
    log_error "Project ID not set in $TFVARS_FILE. Please update it."
    exit 1
fi

if [ -z "$REGION" ]; then
    REGION="europe-west1"
    log_warn "Region not set in $TFVARS_FILE, defaulting to $REGION"
fi

ARTIFACT_REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/servantin-${ENVIRONMENT}"

log_info "Project: $PROJECT_ID"
log_info "Region: $REGION"
log_info "Artifact Registry: $ARTIFACT_REGISTRY"

# Set gcloud project
gcloud config set project "$PROJECT_ID" --quiet

# Configure Docker for Artifact Registry
log_info "Configuring Docker authentication..."
gcloud auth configure-docker ${REGION}-docker.pkg.dev --quiet

# Build and push backend
log_info "Building backend JAR and Docker image..."
cd "$ROOT_DIR/backend"
./gradlew bootJar -x test
docker build --platform linux/amd64 -t "${ARTIFACT_REGISTRY}/backend:latest" .
docker push "${ARTIFACT_REGISTRY}/backend:latest"

# Build and push frontend
log_info "Building frontend Docker image..."
cd "$ROOT_DIR/frontend"
docker build \
    --platform linux/amd64 \
    --build-arg NEXT_PUBLIC_API_URL="" \
    -t "${ARTIFACT_REGISTRY}/frontend:latest" .
docker push "${ARTIFACT_REGISTRY}/frontend:latest"

# Apply Terraform
log_info "Applying Terraform configuration..."
cd "$TERRAFORM_DIR"

terraform init -upgrade

if [ -f "environments/${ENVIRONMENT}.tfvars" ]; then
    terraform plan -var-file="environments/${ENVIRONMENT}.tfvars" -out=tfplan
    
    read -p "Apply this plan? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        terraform apply tfplan
    else
        log_warn "Deployment cancelled."
        exit 0
    fi
else
    log_error "Environment file not found: environments/${ENVIRONMENT}.tfvars"
    exit 1
fi

# Get outputs
log_info "Deployment complete!"
echo ""
echo "=========================================="
echo "Deployment Summary"
echo "=========================================="
terraform output
echo ""
log_info "Frontend URL: $(terraform output -raw frontend_url)"
log_info "Backend URL: $(terraform output -raw backend_url)"
