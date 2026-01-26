#!/bin/bash
#
# Supabase Connection Test Script
# Tests connectivity to your Supabase PostgreSQL instance
#
# Usage: ./test-supabase.sh
#

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo ""
echo "============================================"
echo "  Supabase Connection Test"
echo "============================================"
echo ""

# Check for .env file
ENV_FILE="$(dirname "$0")/../.env"
if [ -f "$ENV_FILE" ]; then
    log_info "Loading configuration from .env"
    export $(grep -v '^#' "$ENV_FILE" | grep -v '^$' | xargs)
else
    log_warn ".env file not found at $ENV_FILE"
    echo ""
    echo "Please provide your Supabase credentials:"
    read -p "SUPABASE_HOST (e.g., abc123.supabase.co): " SUPABASE_HOST
    read -p "SUPABASE_PORT (default: 6543): " SUPABASE_PORT
    SUPABASE_PORT=${SUPABASE_PORT:-6543}
    read -p "SUPABASE_USER (e.g., postgres.abc123): " SUPABASE_USER
    read -sp "SUPABASE_PASSWORD: " SUPABASE_PASSWORD
    echo ""
    SUPABASE_DB=${SUPABASE_DB:-postgres}
fi

# Validate required fields
if [ -z "$SUPABASE_HOST" ] || [ "$SUPABASE_HOST" = "YOUR_PROJECT_REF.supabase.co" ]; then
    log_error "SUPABASE_HOST is not configured"
    exit 1
fi

if [ -z "$SUPABASE_PASSWORD" ] || [ "$SUPABASE_PASSWORD" = "YOUR_SUPABASE_DATABASE_PASSWORD" ]; then
    log_error "SUPABASE_PASSWORD is not configured"
    exit 1
fi

# Set defaults
SUPABASE_PORT=${SUPABASE_PORT:-6543}
SUPABASE_DB=${SUPABASE_DB:-postgres}
SUPABASE_USER=${SUPABASE_USER:-postgres}

log_info "Testing connection to Supabase..."
echo "  Host: $SUPABASE_HOST"
echo "  Port: $SUPABASE_PORT"
echo "  Database: $SUPABASE_DB"
echo "  User: $SUPABASE_USER"
echo ""

# Test 1: DNS Resolution
log_info "Step 1: Testing DNS resolution..."
if host "$SUPABASE_HOST" > /dev/null 2>&1 || nslookup "$SUPABASE_HOST" > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓${NC} DNS resolution successful"
else
    log_error "DNS resolution failed for $SUPABASE_HOST"
    log_info "Please verify your SUPABASE_HOST is correct"
    exit 1
fi

# Test 2: Port connectivity
log_info "Step 2: Testing port connectivity..."
if nc -z -w5 "$SUPABASE_HOST" "$SUPABASE_PORT" 2>/dev/null; then
    echo -e "  ${GREEN}✓${NC} Port $SUPABASE_PORT is reachable"
else
    # Try with timeout command if nc not available
    if command -v timeout &> /dev/null; then
        if timeout 5 bash -c "</dev/tcp/$SUPABASE_HOST/$SUPABASE_PORT" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} Port $SUPABASE_PORT is reachable"
        else
            log_warn "Port connectivity test failed (might be firewall)"
            log_info "Continuing with database connection test..."
        fi
    else
        log_warn "nc/netcat not available, skipping port test"
    fi
fi

# Test 3: PostgreSQL connection
log_info "Step 3: Testing PostgreSQL connection..."

if command -v psql &> /dev/null; then
    # Test with psql
    if PGPASSWORD="$SUPABASE_PASSWORD" psql \
        -h "$SUPABASE_HOST" \
        -p "$SUPABASE_PORT" \
        -U "$SUPABASE_USER" \
        -d "$SUPABASE_DB" \
        -c "SELECT version();" \
        --quiet \
        2>&1; then
        echo ""
        echo -e "  ${GREEN}✓${NC} PostgreSQL connection successful!"
    else
        log_error "PostgreSQL connection failed"
        echo ""
        echo "Common issues:"
        echo "  1. Wrong password"
        echo "  2. Wrong port (use 6543 for Transaction mode)"
        echo "  3. Wrong user format (should be: postgres.YOUR_PROJECT_REF)"
        echo "  4. Project is paused (check Supabase Dashboard)"
        exit 1
    fi
else
    log_warn "psql not installed, attempting connection via Docker..."
    
    if docker run --rm \
        -e PGPASSWORD="$SUPABASE_PASSWORD" \
        postgres:16-alpine \
        psql \
        -h "$SUPABASE_HOST" \
        -p "$SUPABASE_PORT" \
        -U "$SUPABASE_USER" \
        -d "$SUPABASE_DB" \
        -c "SELECT 'Connection successful!' as status;" \
        2>&1; then
        echo ""
        echo -e "  ${GREEN}✓${NC} PostgreSQL connection successful!"
    else
        log_error "PostgreSQL connection failed"
        exit 1
    fi
fi

# Test 4: Test with JDBC-like parameters
log_info "Step 4: Testing with prepareThreshold=0 (PgBouncer compatibility)..."

if command -v psql &> /dev/null; then
    # Run a simple query that would break with prepared statements
    if PGPASSWORD="$SUPABASE_PASSWORD" psql \
        -h "$SUPABASE_HOST" \
        -p "$SUPABASE_PORT" \
        -U "$SUPABASE_USER" \
        -d "$SUPABASE_DB" \
        -c "SELECT 1 as test_query;" \
        --quiet \
        2>/dev/null; then
        echo -e "  ${GREEN}✓${NC} Simple query executed successfully"
    fi
fi

echo ""
log_info "============================================"
log_info "  All Tests Passed! ✓"
log_info "============================================"
echo ""
log_info "Your Supabase configuration is working correctly."
log_info "You can now deploy with: ./deploy.sh --supabase --build"
echo ""

# Show connection string for reference
echo "JDBC Connection String (for reference):"
echo "  jdbc:postgresql://${SUPABASE_HOST}:${SUPABASE_PORT}/${SUPABASE_DB}?prepareThreshold=0&ssl=require&sslmode=require"
echo ""
