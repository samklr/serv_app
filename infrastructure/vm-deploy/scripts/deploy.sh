#!/bin/bash
#
# Servantin Deployment Script
# Deploys the application to a single VM
#
# Usage: ./deploy.sh [options]
# Options:
#   --build     Force rebuild images (no cache)
#   --pull      Pull latest from git
#   --clean     Clean up old images after deployment
#   --supabase  Use Supabase PostgreSQL instead of local PostgreSQL
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$(dirname "$DEPLOY_DIR")")"
APP_DIR="/opt/servantin"
ENV_FILE="${DEPLOY_DIR}/.env"

# Parse arguments
BUILD=false
PULL=false
CLEAN=false
USE_SUPABASE=false

for arg in "$@"; do
    case $arg in
        --build) BUILD=true ;;
        --pull) PULL=true ;;
        --clean) CLEAN=true ;;
        --supabase) USE_SUPABASE=true ;;
        *) log_warn "Unknown argument: $arg" ;;
    esac
done

# Set compose file based on mode
if [ "$USE_SUPABASE" = true ]; then
    COMPOSE_FILE="docker-compose.supabase.yml"
    DEPLOY_MODE="Supabase"
else
    COMPOSE_FILE="docker-compose.yml"
    DEPLOY_MODE="Local PostgreSQL"
fi

# Banner
echo ""
echo "============================================"
echo "  Servantin Deployment Script"
echo "  Mode: ${DEPLOY_MODE}"
echo "============================================"
echo ""

# Check prerequisites
log_step "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed. Run setup-vm.sh first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    log_error "Docker Compose is not installed. Run setup-vm.sh first."
    exit 1
fi

# Check for .env file
if [ ! -f "$ENV_FILE" ]; then
    log_error ".env file not found at $ENV_FILE"
    if [ "$USE_SUPABASE" = true ]; then
        log_info "Copy .env.supabase.example to .env and configure it:"
        log_info "  cp ${DEPLOY_DIR}/.env.supabase.example ${ENV_FILE}"
    else
        log_info "Copy .env.example to .env and configure it:"
        log_info "  cp ${DEPLOY_DIR}/.env.example ${ENV_FILE}"
    fi
    exit 1
fi

# Load environment variables for validation
export $(grep -v '^#' "$ENV_FILE" | grep -v '^$' | xargs)

# Validate Supabase configuration if using Supabase
if [ "$USE_SUPABASE" = true ]; then
    log_step "Validating Supabase configuration..."
    
    if [ -z "$SUPABASE_HOST" ] || [ "$SUPABASE_HOST" = "YOUR_PROJECT_REF.supabase.co" ]; then
        log_error "SUPABASE_HOST is not configured in .env"
        exit 1
    fi
    
    if [ -z "$SUPABASE_PASSWORD" ] || [ "$SUPABASE_PASSWORD" = "YOUR_SUPABASE_DATABASE_PASSWORD" ]; then
        log_error "SUPABASE_PASSWORD is not configured in .env"
        exit 1
    fi
    
    if [ -z "$SUPABASE_USER" ] || [ "$SUPABASE_USER" = "postgres.YOUR_PROJECT_REF" ]; then
        log_error "SUPABASE_USER is not configured in .env"
        exit 1
    fi
    
    # Test Supabase connectivity
    log_step "Testing Supabase connectivity..."
    if command -v psql &> /dev/null; then
        if PGPASSWORD="$SUPABASE_PASSWORD" psql -h "$SUPABASE_HOST" -p "${SUPABASE_PORT:-6543}" -U "$SUPABASE_USER" -d "${SUPABASE_DB:-postgres}" -c "SELECT 1;" > /dev/null 2>&1; then
            log_info "Supabase connection successful"
        else
            log_warn "Could not verify Supabase connection (psql test failed)"
            log_warn "This might be a network issue or incorrect credentials"
            log_info "Continuing with deployment..."
        fi
    else
        log_warn "psql not installed, skipping Supabase connectivity test"
        log_info "Connection will be tested when backend starts"
    fi
    
    log_info "Supabase configuration validated"
fi

log_info "Prerequisites check passed"

# Pull latest code if requested
if [ "$PULL" = true ]; then
    log_step "Pulling latest code from git..."
    cd "$PROJECT_DIR"
    git pull
    log_info "Code updated"
fi

# Build Docker images
# We build from the deployment directory so that relative paths (../../backend) resolve correctly.
# We also enable BuildKit and allow filesystem access to prevent permission errors like "requesting privileges for fs.read=/".
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
export BUILDX_BAKE_ENTITLEMENTS_FS=0

# Ensure environment variables are loaded for the build
if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | grep -v '^$' | xargs)
fi

if [ "$BUILD" = true ]; then
    log_step "Building Docker images (no cache)..."
    docker-compose -f "${DEPLOY_DIR}/${COMPOSE_FILE}" --project-directory "${DEPLOY_DIR}" build --no-cache
else
    log_step "Building Docker images (cached)..."
    docker-compose -f "${DEPLOY_DIR}/${COMPOSE_FILE}" --project-directory "${DEPLOY_DIR}" build
fi

# Create directories
log_step "Creating directories..."
mkdir -p "$APP_DIR"/{data,logs,backups,certs}
mkdir -p "${DEPLOY_DIR}/certs"

# Copy files to app directory
log_step "Copying configuration files..."
cp "${DEPLOY_DIR}/${COMPOSE_FILE}" "$APP_DIR/docker-compose.yml"
cp "${ENV_FILE}" "$APP_DIR/.env"
cp -r "${DEPLOY_DIR}/config" "$APP_DIR/"

# Switch to app directory for runtime operations
cd "$APP_DIR"
# Load the .env file from the app directory that we just copied
export $(grep -v '^#' .env | grep -v '^$' | xargs)

# Stop existing containers gracefully
log_step "Stopping existing containers..."
docker-compose -f docker-compose.yml down --remove-orphans || true

# Start services
log_step "Starting services..."
docker-compose -f docker-compose.yml up -d

# Wait for services to be healthy
log_step "Waiting for services to be healthy..."

# Only wait for PostgreSQL if NOT using Supabase
if [ "$USE_SUPABASE" != true ]; then
    echo -n "Waiting for PostgreSQL..."
    timeout=60
    counter=0
    until docker-compose exec -T postgres pg_isready -U ${DB_USER:-servantin} > /dev/null 2>&1; do
        sleep 2
        counter=$((counter + 2))
        if [ $counter -ge $timeout ]; then
            echo " TIMEOUT"
            log_error "PostgreSQL failed to start within ${timeout}s"
            exit 1
        fi
        echo -n "."
    done
    echo " OK"
fi

echo -n "Waiting for Backend..."
timeout=120  # Longer timeout for Supabase (remote DB connection)
counter=0
until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do
    sleep 5
    counter=$((counter + 5))
    if [ $counter -ge $timeout ]; then
        echo " TIMEOUT"
        log_warn "Backend may still be starting..."
        log_info "Check logs with: ./manage.sh logs -f backend"
        break
    fi
    echo -n "."
done
echo " OK"

echo -n "Waiting for Frontend..."
timeout=60
counter=0
until curl -sf http://localhost:3000 > /dev/null 2>&1; do
    sleep 3
    counter=$((counter + 3))
    if [ $counter -ge $timeout ]; then
        echo " TIMEOUT"
        log_warn "Frontend may still be starting..."
        break
    fi
    echo -n "."
done
echo " OK"

# Clean up old images if requested
if [ "$CLEAN" = true ]; then
    log_step "Cleaning up old images..."
    docker image prune -f
fi

# Print status
echo ""
log_info "============================================"
log_info "  Deployment Complete!"
log_info "  Mode: ${DEPLOY_MODE}"
log_info "============================================"
echo ""
log_info "Services status:"
docker-compose -f docker-compose.yml ps
echo ""
log_info "Application URLs:"
echo "  - Frontend: https://${DOMAIN:-servapp.latticeiq.net}"
echo "  - Backend API: https://${DOMAIN:-servapp.latticeiq.net}/api"
echo "  - Swagger UI: https://${DOMAIN:-servapp.latticeiq.net}/swagger-ui.html"
echo ""
log_info "SSL certificates are automatically managed by Caddy (Let's Encrypt)"
echo ""

if [ "$USE_SUPABASE" = true ]; then
    log_info "Database: Supabase PostgreSQL (${SUPABASE_HOST}:${SUPABASE_PORT:-6543})"
    log_info "Note: Database backups should be managed via Supabase Dashboard"
else
    log_info "Database: Local PostgreSQL container"
fi
echo ""
log_info "Management commands:"
echo "  - View logs: ./manage.sh logs"
echo "  - Stop app: ./manage.sh stop"
echo "  - Restart: ./manage.sh restart"
echo "  - Status: ./manage.sh status"
echo ""
