# Servantin Infrastructure

Terraform configurations for deploying Servantin to Google Cloud Platform.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           GOOGLE CLOUD PLATFORM                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Cloud Run (Serverless)                        │   │
│  │                                                                       │   │
│  │   ┌─────────────────────┐      ┌──────────────────────┐             │   │
│  │   │  servantin-web      │      │  servantin-api       │             │   │
│  │   │  ┌───────────────┐  │      │  ┌────────────────┐  │             │   │
│  │   │  │   Next.js     │  │ ───► │  │  Spring Boot   │  │             │   │
│  │   │  │   Frontend    │  │      │  │    Backend     │  │             │   │
│  │   │  └───────────────┘  │      │  └────────────────┘  │             │   │
│  │   │                     │      │          │           │             │   │
│  │   │   Port: 3000        │      │   Port: 8080         │             │   │
│  │   │   Min: 0/1          │      │   Min: 0/1           │             │   │
│  │   │   Max: 5/10         │      │   Max: 5/10          │             │   │
│  │   └─────────────────────┘      └──────────┬───────────┘             │   │
│  │                                           │                          │   │
│  └───────────────────────────────────────────┼──────────────────────────┘   │
│                                              │                              │
│  ┌───────────────────────────────────────────┼──────────────────────────┐   │
│  │             VPC Access Connector          │                          │   │
│  │             (10.8.0.0/28)                 │                          │   │
│  └───────────────────────────────────────────┼──────────────────────────┘   │
│                                              │                              │
│  ┌───────────────────────────────────────────┼──────────────────────────┐   │
│  │                    Private VPC (10.0.0.0/24)                         │   │
│  │                                           │                          │   │
│  │                             ┌─────────────▼─────────────┐            │   │
│  │                             │       Cloud SQL           │            │   │
│  │                             │     PostgreSQL 16         │            │   │
│  │                             │                           │            │   │
│  │                             │   • Private IP only       │            │   │
│  │                             │   • Auto backups          │            │   │
│  │                             │   • Point-in-time recovery│            │   │
│  │                             └───────────────────────────┘            │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐   │
│  │                       Supporting Services                             │   │
│  │                                                                       │   │
│  │   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │   │
│  │   │ Secret Manager  │  │   Artifact      │  │   Cloud Storage     │  │   │
│  │   │                 │  │   Registry      │  │                     │  │   │
│  │   │ • DB Password   │  │                 │  │   User uploads      │  │   │
│  │   │ • JWT Secret    │  │   Docker        │  │   bucket            │  │   │
│  │   │ • Stripe Key    │  │   images        │  │                     │  │   │
│  │   └─────────────────┘  └─────────────────┘  └─────────────────────┘  │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
infrastructure/
├── README.md               # This file
└── terraform/
    ├── main.tf             # Main resource definitions
    ├── variables.tf        # Input variables with validation
    ├── outputs.tf          # Output values
    ├── versions.tf         # Provider version constraints
    ├── .gitignore          # Ignore state files and secrets
    └── environments/
        ├── dev.tfvars      # Development configuration
        ├── staging.tfvars  # Staging configuration
        └── prod.tfvars     # Production configuration
```

## Quick Start

### Prerequisites

1. [Google Cloud SDK](https://cloud.google.com/sdk/docs/install)
2. [Terraform](https://terraform.io/downloads) >= 1.5.0
3. A GCP project with billing enabled

### 1. Setup GCP Project

```bash
# From repository root
./scripts/setup-gcp.sh YOUR_PROJECT_ID
```

### 2. Configure Environment

```bash
# Edit the configuration
vi terraform/environments/dev.tfvars

# Required changes:
# - project_id = "your-actual-project-id"
```

### 3. Initialize Terraform

```bash
cd terraform
terraform init
```

### 4. Deploy

```bash
# Plan first (review changes)
terraform plan -var-file=environments/dev.tfvars

# Apply
terraform apply -var-file=environments/dev.tfvars
```

### 5. Get Outputs

```bash
terraform output frontend_url
terraform output backend_url
terraform output deployment_summary
```

## Environments

### Development (`dev.tfvars`)

- Scale to zero (cost savings)
- Minimal database tier
- 7-day backups
- Unencrypted DB connections allowed

### Staging (`staging.tfvars`)  

- Scale to zero (cost savings)
- Small database tier
- 7-day backups
- Mirrors production features

### Production (`prod.tfvars`)

- Minimum 1 instance (always warm)
- HA database with regional availability
- 30-day backups with PITR
- SSL-only database connections
- Deletion protection enabled

## Resources Created

| Resource | Name Pattern | Purpose |
|----------|--------------|---------|
| VPC Network | `servantin-{env}-vpc` | Private networking |
| Subnet | `servantin-{env}-subnet` | IP allocation |
| VPC Connector | `servantin-{env}-connector` | Cloud Run → VPC access |
| Cloud SQL | `servantin-{env}-db-{suffix}` | PostgreSQL database |
| Secret (DB Password) | `servantin-{env}-db-password` | Database credentials |
| Secret (JWT) | `servantin-{env}-jwt-secret` | JWT signing key |
| Secret (Stripe) | `servantin-{env}-stripe-api-key` | Payment API key |
| Service Account | `servantin-{env}-run` | Cloud Run identity |
| Artifact Registry | `servantin-{env}` | Docker images |
| Cloud Run (API) | `servantin-{env}-api` | Backend service |
| Cloud Run (Web) | `servantin-{env}-web` | Frontend service |
| Storage Bucket | `{project}-servantin-{env}-uploads` | User uploads |

## Configuration

### Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `project_id` | GCP Project ID | Required |
| `region` | GCP Region | `europe-west1` |
| `environment` | Environment name | `dev` |
| `db_tier` | Cloud SQL tier | `db-f1-micro` |
| `db_disk_size` | Database disk (GB) | `10` |
| `backend_cpu` | Backend CPU | `1` |
| `backend_memory` | Backend memory | `512Mi` |
| `frontend_cpu` | Frontend CPU | `1` |
| `frontend_memory` | Frontend memory | `256Mi` |

See `variables.tf` for complete list with validation rules.

### Labels

All resources are tagged with:

```hcl
labels = {
  project     = "servantin"
  environment = var.environment
  managed_by  = "terraform"
  team        = var.team_name
}
```

## Operations

### Update Container Images

```bash
# Update backend only
gcloud run deploy servantin-dev-api \
  --image europe-west1-docker.pkg.dev/PROJECT/servantin-dev/backend:NEW_TAG \
  --region europe-west1

# Update frontend only  
gcloud run deploy servantin-dev-web \
  --image europe-west1-docker.pkg.dev/PROJECT/servantin-dev/frontend:NEW_TAG \
  --region europe-west1
```

### Scale Services

```bash
# Set minimum instances (for warm starts)
gcloud run services update servantin-dev-api \
  --min-instances=1 \
  --region europe-west1

# Set maximum instances (for load)
gcloud run services update servantin-dev-api \
  --max-instances=20 \
  --region europe-west1
```

### Rotate Secrets

```bash
# Add new version of a secret
echo "new-secret-value" | gcloud secrets versions add servantin-dev-jwt-secret --data-file=-

# Services will pick up new version on next cold start
# Force restart to apply immediately:
gcloud run services update servantin-dev-api \
  --update-env-vars="RESTART_TRIGGER=$(date +%s)" \
  --region europe-west1
```

### Database Access

```bash
# Connect via Cloud SQL Proxy
gcloud sql connect servantin-dev-db-XXXX --user=servantin

# Or use Cloud Shell
gcloud cloud-shell ssh --authorize-session
```

## Troubleshooting

### Terraform State Issues

```bash
# Unlock if stuck
terraform force-unlock LOCK_ID

# Refresh state
terraform refresh -var-file=environments/dev.tfvars
```

### API Enablement Slow

Some APIs take time to enable. Run `terraform apply` again if you see errors about APIs not being enabled.

### VPC Connector Issues

```bash
# Check connector status
gcloud compute networks vpc-access connectors describe servantin-dev-connector --region=europe-west1

# Recreate if stuck (requires taint)
terraform taint google_vpc_access_connector.main
terraform apply -var-file=environments/dev.tfvars
```

### Database Connection Issues

```bash
# Verify private IP is assigned
gcloud sql instances describe servantin-dev-db-XXXX --format="value(ipAddresses)"

# Check VPC peering
gcloud compute networks peerings list --network=servantin-dev-vpc
```

## Cost Estimation

| Environment | Monthly Estimate |
|-------------|------------------|
| Development | $15 - $30 |
| Staging | $30 - $60 |
| Production | $100 - $300 |

Major cost drivers:
- Cloud SQL (largest component)
- Cloud Run (pay per request + compute time)
- VPC Connector (small fixed cost in prod)

## Cleanup

```bash
# Destroy all resources (DANGEROUS!)
terraform destroy -var-file=environments/dev.tfvars

# Remove specific resource
terraform destroy -target=google_cloud_run_v2_service.frontend -var-file=environments/dev.tfvars
```

⚠️ **Warning**: Production has `deletion_protection = true` on the database. You must manually disable this before destroying.

## Best Practices

1. **State Management**: Use remote state (GCS) for team environments
2. **Secrets**: Never commit `.tfvars` files with secrets
3. **Versioning**: Use specific image tags in production
4. **Review**: Always run `terraform plan` before `apply`
5. **Labels**: Ensure all resources are properly labeled for cost tracking
