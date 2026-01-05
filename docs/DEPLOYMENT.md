# Servantin Deployment Guide

This guide covers the complete deployment process for Servantin on Google Cloud Platform (GCP) using Cloud Run, Cloud SQL, and supporting services.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Architecture Overview](#architecture-overview)
3. [Quick Start](#quick-start)
4. [Detailed Setup](#detailed-setup)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Environment Management](#environment-management)
7. [Monitoring & Logging](#monitoring--logging)
8. [Troubleshooting](#troubleshooting)
9. [Cost Management](#cost-management)
10. [Security Best Practices](#security-best-practices)

---

## Prerequisites

### Required Tools

| Tool | Version | Installation |
|------|---------|--------------|
| Google Cloud SDK | Latest | [Install Guide](https://cloud.google.com/sdk/docs/install) |
| Terraform | >= 1.5.0 | [Install Guide](https://terraform.io/downloads) |
| Docker | Latest | [Install Guide](https://docs.docker.com/get-docker/) |
| Node.js | >= 20.x | [Install Guide](https://nodejs.org/) |
| Java JDK | 21 | [Install Guide](https://adoptium.net/) |

### GCP Account Setup

1. **Create a GCP Account** at [console.cloud.google.com](https://console.cloud.google.com)
2. **Create a new Project** or use an existing one
3. **Enable Billing** for the project
4. **Install and authenticate gcloud**:

```bash
# Install gcloud (macOS)
brew install google-cloud-sdk

# Authenticate
gcloud auth login
gcloud auth application-default login

# Set your project
gcloud config set project YOUR_PROJECT_ID
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              INTERNET                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
        ┌───────────────────────┐     ┌───────────────────────┐
        │   Cloud Run (Web)     │     │   Cloud Run (API)     │
        │   servantin-web       │────▶│   servantin-api       │
        │   Next.js Frontend    │     │   Spring Boot Backend │
        │   Port: 3000          │     │   Port: 8080          │
        └───────────────────────┘     └───────────┬───────────┘
                                                  │
                                    ┌─────────────┴─────────────┐
                                    │    VPC Access Connector    │
                                    └─────────────┬─────────────┘
                                                  │
┌─────────────────────────────────────────────────┼─────────────────────────────┐
│                           PRIVATE VPC           │                             │
│                                    ┌────────────┴────────────┐                │
│                                    │       Cloud SQL         │                │
│                                    │     PostgreSQL 16       │                │
│                                    │   (Private IP only)     │                │
│                                    └─────────────────────────┘                │
└───────────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────┐
│                          SUPPORTING SERVICES                                  │
│                                                                               │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────────────┐    │
│   │ Secret Manager  │   │   Artifact      │   │    Cloud Storage        │    │
│   │ - DB Password   │   │   Registry      │   │    (User Uploads)       │    │
│   │ - JWT Secret    │   │   (Docker       │   │                         │    │
│   │ - Stripe Key    │   │    Images)      │   │                         │    │
│   └─────────────────┘   └─────────────────┘   └─────────────────────────┘    │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

### Components

| Component | Service | Purpose |
|-----------|---------|---------|
| Frontend | Cloud Run | Next.js SSR application |
| Backend | Cloud Run | Spring Boot REST API |
| Database | Cloud SQL | PostgreSQL 16 with private IP |
| Secrets | Secret Manager | Sensitive configuration storage |
| Container Registry | Artifact Registry | Docker image storage |
| File Storage | Cloud Storage | User uploads (photos, documents) |
| Networking | VPC + Connector | Secure private connectivity |

---

## Quick Start

### 1. Clone and Setup

```bash
# Clone the repository
git clone https://github.com/your-org/servantin.git
cd servantin

# Make scripts executable
chmod +x scripts/*.sh
```

### 2. Initialize GCP Project

```bash
# Run the setup script with your project ID
./scripts/setup-gcp.sh YOUR_PROJECT_ID

# This will:
# - Enable required APIs
# - Create Artifact Registry repository
# - Create Terraform state bucket
# - Configure Cloud Build permissions
```

### 3. Configure Environment

```bash
# Edit the development configuration
vi infrastructure/terraform/environments/dev.tfvars

# Update at minimum:
# - project_id = "your-project-id"
# - stripe_api_key = "sk_test_your_key"
```

### 4. Deploy

```bash
# Deploy to development
./scripts/deploy.sh dev

# This will:
# - Build Docker images
# - Push to Artifact Registry
# - Apply Terraform configuration
# - Deploy to Cloud Run
```

### 5. Access Your Application

After deployment, Terraform outputs the URLs:

```bash
cd infrastructure/terraform
terraform output frontend_url
terraform output backend_url
```

---

## Detailed Setup

### Step 1: Enable GCP APIs

The following APIs must be enabled:

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  vpcaccess.googleapis.com \
  servicenetworking.googleapis.com \
  compute.googleapis.com
```

### Step 2: Create Artifact Registry

```bash
gcloud artifacts repositories create servantin-dev \
  --repository-format=docker \
  --location=europe-west1 \
  --description="Servantin Docker images"
```

### Step 3: Configure Docker Authentication

```bash
gcloud auth configure-docker europe-west1-docker.pkg.dev
```

### Step 4: Build and Push Images

#### Backend

```bash
cd backend

# Build the image
docker build -t europe-west1-docker.pkg.dev/YOUR_PROJECT/servantin-dev/backend:latest .

# Push to registry
docker push europe-west1-docker.pkg.dev/YOUR_PROJECT/servantin-dev/backend:latest
```

#### Frontend

```bash
cd frontend

# Build with API URL (empty for proxy mode)
docker build \
  --build-arg NEXT_PUBLIC_API_URL="" \
  -t europe-west1-docker.pkg.dev/YOUR_PROJECT/servantin-dev/frontend:latest .

# Push to registry
docker push europe-west1-docker.pkg.dev/YOUR_PROJECT/servantin-dev/frontend:latest
```

### Step 5: Deploy with Terraform

```bash
cd infrastructure/terraform

# Initialize Terraform
terraform init

# Review the plan
terraform plan -var-file=environments/dev.tfvars

# Apply (confirm with 'yes')
terraform apply -var-file=environments/dev.tfvars
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to GCP

on:
  push:
    branches: [main, develop]
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to deploy to'
        required: true
        default: 'dev'
        type: choice
        options:
          - dev
          - staging
          - prod

env:
  PROJECT_ID: ${{ secrets.GCP_PROJECT_ID }}
  REGION: europe-west1
  
jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment || 'dev' }}
    
    permissions:
      contents: read
      id-token: write
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Authenticate to GCP
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ secrets.WIF_PROVIDER }}
          service_account: ${{ secrets.WIF_SERVICE_ACCOUNT }}
      
      - name: Set up Cloud SDK
        uses: google-github-actions/setup-gcloud@v2
      
      - name: Configure Docker
        run: gcloud auth configure-docker ${{ env.REGION }}-docker.pkg.dev
      
      - name: Build Backend
        run: |
          docker build -t ${{ env.REGION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/servantin-${{ github.event.inputs.environment || 'dev' }}/backend:${{ github.sha }} ./backend
          docker push ${{ env.REGION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/servantin-${{ github.event.inputs.environment || 'dev' }}/backend:${{ github.sha }}
      
      - name: Build Frontend
        run: |
          docker build \
            --build-arg NEXT_PUBLIC_API_URL="" \
            -t ${{ env.REGION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/servantin-${{ github.event.inputs.environment || 'dev' }}/frontend:${{ github.sha }} \
            ./frontend
          docker push ${{ env.REGION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/servantin-${{ github.event.inputs.environment || 'dev' }}/frontend:${{ github.sha }}
      
      - name: Deploy Backend
        run: |
          gcloud run deploy servantin-${{ github.event.inputs.environment || 'dev' }}-api \
            --image ${{ env.REGION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/servantin-${{ github.event.inputs.environment || 'dev' }}/backend:${{ github.sha }} \
            --region ${{ env.REGION }} \
            --platform managed
      
      - name: Deploy Frontend
        run: |
          gcloud run deploy servantin-${{ github.event.inputs.environment || 'dev' }}-web \
            --image ${{ env.REGION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/servantin-${{ github.event.inputs.environment || 'dev' }}/frontend:${{ github.sha }} \
            --region ${{ env.REGION }} \
            --platform managed
```

### Required GitHub Secrets

| Secret | Description |
|--------|-------------|
| `GCP_PROJECT_ID` | Your GCP project ID |
| `WIF_PROVIDER` | Workload Identity Federation provider |
| `WIF_SERVICE_ACCOUNT` | Service account for deployments |

### Setting up Workload Identity Federation

```bash
# Create a Workload Identity Pool
gcloud iam workload-identity-pools create "github" \
  --location="global" \
  --display-name="GitHub Actions Pool"

# Create a provider
gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --location="global" \
  --workload-identity-pool="github" \
  --display-name="GitHub Provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository" \
  --issuer-uri="https://token.actions.githubusercontent.com"

# Create a service account for GitHub Actions
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions"

# Grant necessary roles
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

# Allow GitHub to impersonate the service account
gcloud iam service-accounts add-iam-policy-binding \
  github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/your-org/servantin"
```

---

## Environment Management

### Environment Comparison

| Setting | Dev | Staging | Prod |
|---------|-----|---------|------|
| Min Instances | 0 | 0 | 1 |
| Max Instances | 2 | 5 | 10 |
| DB Tier | db-f1-micro | db-g1-small | db-custom-2-4096 |
| DB Size | 10 GB | 20 GB | 50 GB |
| Backups | 7 days | 7 days | 30 days |
| Point-in-time Recovery | No | No | Yes |
| SSL Mode | Unencrypted OK | Unencrypted OK | Encrypted Only |
| Scale to Zero | Yes | Yes | No |

### Deploying to Different Environments

```bash
# Development
./scripts/deploy.sh dev

# Staging
./scripts/deploy.sh staging

# Production (requires confirmation)
./scripts/deploy.sh prod
```

### Environment Variables

#### Backend Environment Variables

| Variable | Description | Source |
|----------|-------------|--------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | Terraform |
| `DATABASE_URL` | JDBC connection string | Terraform |
| `DATABASE_USER` | Database username | Terraform |
| `DATABASE_PASSWORD` | Database password | Secret Manager |
| `JWT_SECRET` | JWT signing key | Secret Manager |
| `STRIPE_API_KEY` | Stripe API key | Secret Manager |
| `STORAGE_BUCKET` | GCS bucket name | Terraform |

#### Frontend Environment Variables

| Variable | Description | Source |
|----------|-------------|--------|
| `NODE_ENV` | Node environment | Terraform |
| `NEXT_PUBLIC_API_URL` | API URL (empty for proxy) | Build arg |
| `BACKEND_INTERNAL_URL` | Internal backend URL | Terraform |

---

## Monitoring & Logging

### Cloud Logging

View logs in the GCP Console or via CLI:

```bash
# Backend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=servantin-dev-api" --limit=50

# Frontend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=servantin-dev-web" --limit=50

# Database logs
gcloud logging read "resource.type=cloudsql_database" --limit=50
```

### Cloud Monitoring

#### Key Metrics to Monitor

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| Request Count | Total HTTP requests | N/A (baseline) |
| Request Latency | p50, p95, p99 latency | p95 > 2s |
| Error Rate | 4xx & 5xx responses | > 1% |
| Instance Count | Running containers | Max approached |
| CPU Utilization | Container CPU usage | > 80% |
| Memory Utilization | Container memory usage | > 80% |
| Database Connections | Active connections | > 80 |

#### Creating Alerts

```bash
# Create an alert policy for high error rate
gcloud alpha monitoring policies create \
  --display-name="High Error Rate - Servantin API" \
  --condition-display-name="Error rate > 1%" \
  --notification-channels=YOUR_CHANNEL_ID
```

### Health Checks

The backend exposes health endpoints:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Overall health status |
| `/actuator/health/liveness` | Liveness probe |
| `/actuator/health/readiness` | Readiness probe |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Prometheus metrics |

---

## Troubleshooting

### Common Issues

#### 1. Container Won't Start

**Symptoms**: Cloud Run deployment fails, container crashes

**Check logs**:
```bash
gcloud run services logs read servantin-dev-api --region=europe-west1 --limit=100
```

**Common causes**:
- Missing environment variables
- Database connection failure
- Insufficient memory

#### 2. Database Connection Errors

**Symptoms**: `Connection refused` or timeout errors

**Verify VPC connector**:
```bash
gcloud compute networks vpc-access connectors describe servantin-dev-connector --region=europe-west1
```

**Check Cloud SQL is accepting connections**:
```bash
gcloud sql instances describe servantin-dev-db-XXXX --format="value(settings.ipConfiguration.privateNetwork)"
```

#### 3. Secret Access Denied

**Symptoms**: `Permission denied` accessing secrets

**Verify IAM bindings**:
```bash
gcloud secrets get-iam-policy servantin-dev-db-password
```

**Grant access if needed**:
```bash
gcloud secrets add-iam-policy-binding servantin-dev-db-password \
  --member="serviceAccount:servantin-dev-run@YOUR_PROJECT.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

#### 4. Build Failures

**Symptoms**: Docker build fails

**Check Node.js version** (frontend):
```bash
node --version  # Should be >= 20
```

**Check Java version** (backend):
```bash
java --version  # Should be 21
```

#### 5. Terraform State Issues

**Symptoms**: State lock or corruption

**Unlock state**:
```bash
terraform force-unlock LOCK_ID
```

**Refresh state**:
```bash
terraform refresh -var-file=environments/dev.tfvars
```

### Getting Help

1. Check Cloud Run logs in the Console
2. Review Cloud SQL connection logs
3. Verify Secret Manager access
4. Check IAM permissions

---

## Cost Management

### Estimated Monthly Costs

| Environment | Estimated Cost |
|-------------|----------------|
| Development | $15 - $30 |
| Staging | $30 - $60 |
| Production | $100 - $300 |

### Cost Breakdown

| Service | Dev | Staging | Prod |
|---------|-----|---------|------|
| Cloud Run (API) | $5 | $15 | $50 |
| Cloud Run (Web) | $5 | $10 | $30 |
| Cloud SQL | $8 | $25 | $150 |
| VPC Connector | $0 | $0 | $20 |
| Secret Manager | $0 | $0 | $1 |
| Storage | $1 | $5 | $20 |
| Networking | $1 | $5 | $30 |

### Cost Optimization Tips

1. **Use scale-to-zero** for dev/staging
2. **Right-size database** tier based on actual usage
3. **Enable disk autoresize** with limits
4. **Use lifecycle rules** for Cloud Storage
5. **Monitor unused resources** with Cost Attribution labels

---

## Security Best Practices

### 1. Secrets Management

- ✅ Store all secrets in Secret Manager
- ✅ Never commit secrets to version control
- ✅ Use secret versioning for rotation
- ✅ Grant least-privilege access per secret

### 2. Network Security

- ✅ Database uses private IP only
- ✅ VPC connector for Cloud Run
- ✅ SSL required for database in production
- ✅ No public IP on Cloud SQL

### 3. IAM

- ✅ Dedicated service account for Cloud Run
- ✅ Least-privilege principle
- ✅ No project-level secret access

### 4. Application Security

- ✅ JWT-based authentication
- ✅ HTTPS everywhere
- ✅ CORS configured properly
- ✅ Input validation
- ✅ SQL injection prevention (JPA)

### 5. Compliance

- Consider enabling Cloud Audit Logs
- Enable VPC Flow Logs for network monitoring
- Configure data retention policies
- Implement backup verification

---

## Quick Reference

### Useful Commands

```bash
# Deploy to dev
./scripts/deploy.sh dev

# View service URLs
terraform output -raw frontend_url
terraform output -raw backend_url

# Check service status
gcloud run services describe servantin-dev-api --region=europe-west1

# View logs
gcloud run services logs read servantin-dev-api --region=europe-west1

# Connect to database (for debugging)
gcloud sql connect servantin-dev-db-XXXX --user=servantin

# Update a secret
gcloud secrets versions add servantin-dev-jwt-secret --data-file=new-secret.txt

# Scale a service manually
gcloud run services update servantin-dev-api --min-instances=1 --region=europe-west1

# Rollback to previous revision
gcloud run services update-traffic servantin-dev-api --to-revisions=PREVIOUS_REVISION=100 --region=europe-west1
```

### Important URLs

| Resource | URL |
|----------|-----|
| GCP Console | https://console.cloud.google.com |
| Cloud Run | https://console.cloud.google.com/run |
| Cloud SQL | https://console.cloud.google.com/sql |
| Secret Manager | https://console.cloud.google.com/security/secret-manager |
| Logs | https://console.cloud.google.com/logs |
| Monitoring | https://console.cloud.google.com/monitoring |
