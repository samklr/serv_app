#!/bin/bash
# Initial setup script for Servantin GCP infrastructure
# Run this once to set up the project

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check if project ID is provided
if [ -z "${1:-}" ]; then
    echo "Usage: $0 <project-id>"
    echo "Example: $0 my-gcp-project"
    exit 1
fi

PROJECT_ID="$1"
REGION="${2:-europe-west1}"
REPO_NAME="${3:-servantin}"

log_info "Setting up GCP project: $PROJECT_ID in $REGION with repo $REPO_NAME"

# Set the project
gcloud config set project "$PROJECT_ID" --quiet

# Enable required APIs
log_info "Enabling required GCP APIs..."
APIS=(
    "run.googleapis.com"
    "sqladmin.googleapis.com"
    "secretmanager.googleapis.com"
    "cloudbuild.googleapis.com"
    "artifactregistry.googleapis.com"
    "vpcaccess.googleapis.com"
    "servicenetworking.googleapis.com"
    "compute.googleapis.com"
)

for api in "${APIS[@]}"; do
    log_info "Enabling $api..."
    gcloud services enable "$api" --quiet
done

# Create Artifact Registry repository
log_info "Creating Artifact Registry repository: $REPO_NAME"
gcloud artifacts repositories create "$REPO_NAME" \
    --repository-format=docker \
    --location="$REGION" \
    --description="Docker repository for Servantin" \
    --quiet 2>/dev/null || log_warn "Repository may already exist"

# Create a GCS bucket for Terraform state
STATE_BUCKET="${PROJECT_ID}-terraform-state"
log_info "Creating Terraform state bucket: $STATE_BUCKET"
gsutil mb -p "$PROJECT_ID" -l "$REGION" "gs://${STATE_BUCKET}" 2>/dev/null || log_warn "Bucket may already exist"
gsutil versioning set on "gs://${STATE_BUCKET}"

# Configure Docker authentication
log_info "Configuring Docker for Artifact Registry..."
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# Grant Cloud Build permissions
log_info "Granting Cloud Build permissions..."
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
CLOUD_BUILD_SA="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${CLOUD_BUILD_SA}" \
    --role="roles/run.admin" \
    --quiet

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${CLOUD_BUILD_SA}" \
    --role="roles/iam.serviceAccountUser" \
    --quiet

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${CLOUD_BUILD_SA}" \
    --role="roles/secretmanager.secretAccessor" \
    --quiet

log_info "Setup complete!"
echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Update infrastructure/terraform/environments/dev.tfvars with:"
echo "   project_id = \"$PROJECT_ID\""
echo ""
echo "2. (Optional) Enable Terraform remote state by uncommenting"
echo "   the backend block in main.tf and set bucket to:"
echo "   bucket = \"$STATE_BUCKET\""
echo ""
echo "3. Run: ./scripts/deploy.sh dev"
echo "=========================================="
