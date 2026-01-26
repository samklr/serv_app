#!/bin/bash
#
# Servantin Management Script
# Manage the deployed application
#
# Usage: ./manage.sh <command> [options]
#
# Commands:
#   start       Start all services
#   stop        Stop all services
#   restart     Restart all services
#   status      Show service status
#   logs        View logs (use -f for follow)
#   shell       Open shell in container
#   backup      Create database backup
#   restore     Restore database from backup
#   update      Pull latest and redeploy
#   clean       Clean up Docker resources
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

# Configuration
APP_DIR="/opt/servantin"
BACKUP_DIR="${APP_DIR}/backups"
COMPOSE_FILE="${APP_DIR}/docker-compose.yml"

# Check if compose file exists
check_app() {
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Application not deployed. Run deploy.sh first."
        exit 1
    fi
}

# Load environment
load_env() {
    if [ -f "${APP_DIR}/.env" ]; then
        export $(grep -v '^#' "${APP_DIR}/.env" | grep -v '^$' | xargs)
    fi
}

# Detect if using Supabase (check for SUPABASE_HOST in env)
is_supabase() {
    load_env
    if [ -n "$SUPABASE_HOST" ] && [ "$SUPABASE_HOST" != "YOUR_PROJECT_REF.supabase.co" ]; then
        return 0  # true
    else
        return 1  # false
    fi
}

# Commands
cmd_start() {
    check_app
    load_env
    log_info "Starting services..."
    cd "$APP_DIR"
    docker-compose -f "$COMPOSE_FILE" up -d
    log_info "Services started"
    cmd_status
}

cmd_stop() {
    check_app
    load_env
    log_info "Stopping services..."
    cd "$APP_DIR"
    docker-compose -f "$COMPOSE_FILE" down
    log_info "Services stopped"
}

cmd_restart() {
    check_app
    load_env
    log_info "Restarting services..."
    cd "$APP_DIR"

    if [ -n "$2" ]; then
        docker-compose -f "$COMPOSE_FILE" restart "$2"
        log_info "Service '$2' restarted"
    else
        docker-compose -f "$COMPOSE_FILE" restart
        log_info "All services restarted"
    fi
}

cmd_status() {
    check_app
    load_env
    echo ""
    log_info "Service Status:"
    cd "$APP_DIR"
    docker-compose -f "$COMPOSE_FILE" ps
    echo ""

    log_info "Resource Usage:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" \
        $(docker-compose -f "$COMPOSE_FILE" ps -q) 2>/dev/null || true
    echo ""

    log_info "Disk Usage:"
    df -h /opt/servantin 2>/dev/null || df -h /
    echo ""
    
    # Show database mode
    if is_supabase; then
        log_info "Database Mode: Supabase (${SUPABASE_HOST}:${SUPABASE_PORT:-6543})"
    else
        log_info "Database Mode: Local PostgreSQL"
    fi
    echo ""
}

cmd_logs() {
    check_app
    load_env
    cd "$APP_DIR"

    if [ "$2" = "-f" ] || [ "$2" = "--follow" ]; then
        if [ -n "$3" ]; then
            docker-compose -f "$COMPOSE_FILE" logs -f "$3"
        else
            docker-compose -f "$COMPOSE_FILE" logs -f
        fi
    else
        if [ -n "$2" ]; then
            docker-compose -f "$COMPOSE_FILE" logs --tail=100 "$2"
        else
            docker-compose -f "$COMPOSE_FILE" logs --tail=100
        fi
    fi
}

cmd_shell() {
    check_app
    load_env
    cd "$APP_DIR"

    service="${2:-backend}"
    case $service in
        backend|api)
            docker-compose -f "$COMPOSE_FILE" exec backend sh
            ;;
        frontend|web)
            docker-compose -f "$COMPOSE_FILE" exec frontend sh
            ;;
        postgres|db)
            if is_supabase; then
                # Connect to Supabase PostgreSQL
                log_info "Connecting to Supabase PostgreSQL..."
                if command -v psql &> /dev/null; then
                    PGPASSWORD="$SUPABASE_PASSWORD" psql \
                        -h "$SUPABASE_HOST" \
                        -p "${SUPABASE_PORT:-6543}" \
                        -U "$SUPABASE_USER" \
                        -d "${SUPABASE_DB:-postgres}"
                else
                    log_error "psql is not installed. Install with: apt-get install postgresql-client"
                    log_info "Alternative: Use Supabase Dashboard SQL Editor"
                    exit 1
                fi
            else
                # Connect to local PostgreSQL
                docker-compose -f "$COMPOSE_FILE" exec postgres psql -U ${DB_USER:-servantin} -d ${DB_NAME:-servantin}
            fi
            ;;
        caddy|proxy)
            docker-compose -f "$COMPOSE_FILE" exec caddy sh
            ;;
        *)
            log_error "Unknown service: $service"
            if is_supabase; then
                log_info "Available: backend, frontend, postgres (remote), caddy"
            else
                log_info "Available: backend, frontend, postgres, caddy"
            fi
            exit 1
            ;;
    esac
}

cmd_backup() {
    check_app
    load_env

    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="${BACKUP_DIR}/servantin_${TIMESTAMP}.sql.gz"

    mkdir -p "$BACKUP_DIR"

    if is_supabase; then
        # Backup from Supabase
        log_info "Creating Supabase database backup..."
        log_info "Connecting to ${SUPABASE_HOST}:${SUPABASE_PORT:-6543}..."
        
        if ! command -v pg_dump &> /dev/null; then
            log_error "pg_dump is not installed. Install with: apt-get install postgresql-client"
            log_info "Alternative: Use Supabase Dashboard to export data"
            exit 1
        fi
        
        PGPASSWORD="$SUPABASE_PASSWORD" pg_dump \
            -h "$SUPABASE_HOST" \
            -p "${SUPABASE_PORT:-6543}" \
            -U "$SUPABASE_USER" \
            -d "${SUPABASE_DB:-postgres}" \
            --clean --if-exists \
            --no-owner --no-privileges \
            | gzip > "$BACKUP_FILE"
    else
        # Backup from local PostgreSQL
        log_info "Creating database backup..."
        cd "$APP_DIR"
        docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_dump \
            -U ${DB_USER:-servantin} \
            -d ${DB_NAME:-servantin} \
            --clean --if-exists \
            | gzip > "$BACKUP_FILE"
    fi

    log_info "Backup created: $BACKUP_FILE"
    log_info "Backup size: $(du -h "$BACKUP_FILE" | cut -f1)"

    # Keep only last 7 backups
    cd "$BACKUP_DIR"
    ls -t servantin_*.sql.gz 2>/dev/null | tail -n +8 | xargs -r rm -f
    log_info "Old backups cleaned up (keeping last 7)"
}

cmd_restore() {
    check_app
    load_env

    if [ -z "$2" ]; then
        log_info "Available backups:"
        ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null || echo "No backups found"
        echo ""
        log_info "Usage: ./manage.sh restore <backup_file>"
        exit 1
    fi

    BACKUP_FILE="$2"
    if [ ! -f "$BACKUP_FILE" ]; then
        BACKUP_FILE="${BACKUP_DIR}/$2"
    fi

    if [ ! -f "$BACKUP_FILE" ]; then
        log_error "Backup file not found: $2"
        exit 1
    fi

    log_warn "This will overwrite the current database!"
    if is_supabase; then
        log_warn "Target: Supabase (${SUPABASE_HOST})"
    else
        log_warn "Target: Local PostgreSQL"
    fi
    read -p "Are you sure? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Restore cancelled"
        exit 0
    fi

    log_info "Restoring from: $BACKUP_FILE"
    
    if is_supabase; then
        # Restore to Supabase
        if ! command -v psql &> /dev/null; then
            log_error "psql is not installed. Install with: apt-get install postgresql-client"
            exit 1
        fi
        
        gunzip -c "$BACKUP_FILE" | PGPASSWORD="$SUPABASE_PASSWORD" psql \
            -h "$SUPABASE_HOST" \
            -p "${SUPABASE_PORT:-6543}" \
            -U "$SUPABASE_USER" \
            -d "${SUPABASE_DB:-postgres}"
    else
        # Restore to local PostgreSQL
        cd "$APP_DIR"
        gunzip -c "$BACKUP_FILE" | docker-compose -f "$COMPOSE_FILE" exec -T postgres psql \
            -U ${DB_USER:-servantin} \
            -d ${DB_NAME:-servantin}
    fi

    log_info "Database restored successfully"
}

cmd_update() {
    log_info "Updating application..."
    load_env

    # Create backup first (optional for Supabase, but still good practice)
    log_info "Creating pre-update backup..."
    cmd_backup || log_warn "Backup failed, continuing with update..."

    # Detect if using Supabase and pass the flag
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    if is_supabase; then
        "${SCRIPT_DIR}/deploy.sh" --pull --build --clean --supabase
    else
        "${SCRIPT_DIR}/deploy.sh" --pull --build --clean
    fi
}

cmd_clean() {
    log_info "Cleaning up Docker resources..."

    # Remove stopped containers
    docker container prune -f

    # Remove unused images
    docker image prune -f

    # Remove unused volumes (be careful!)
    if [ "$2" = "--volumes" ]; then
        log_warn "This will remove unused volumes!"
        read -p "Are you sure? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            docker volume prune -f
        fi
    fi

    # Remove unused networks
    docker network prune -f

    log_info "Cleanup complete"
    docker system df
}

cmd_help() {
    echo ""
    echo "Servantin Management Script"
    echo ""
    echo "Usage: ./manage.sh <command> [options]"
    echo ""
    echo "Commands:"
    echo "  start                 Start all services"
    echo "  stop                  Stop all services"
    echo "  restart [service]     Restart all or specific service"
    echo "  status                Show service status and resource usage"
    echo "  logs [-f] [service]   View logs (-f to follow)"
    echo "  shell <service>       Open shell (backend|frontend|postgres|caddy)"
    echo "  backup                Create database backup"
    echo "  restore <file>        Restore database from backup"
    echo "  update                Pull latest and redeploy"
    echo "  clean [--volumes]     Clean up Docker resources"
    echo "  help                  Show this help"
    echo ""
    echo "Examples:"
    echo "  ./manage.sh logs -f backend    Follow backend logs"
    echo "  ./manage.sh shell postgres     Open PostgreSQL shell"
    echo "  ./manage.sh restart caddy      Restart only caddy"
    echo ""
    echo "Database Modes:"
    echo "  - Local PostgreSQL: Uses containerized PostgreSQL"
    echo "  - Supabase: Uses remote Supabase PostgreSQL (auto-detected from .env)"
    echo ""
}

# Main
case "${1:-help}" in
    start)   cmd_start "$@" ;;
    stop)    cmd_stop "$@" ;;
    restart) cmd_restart "$@" ;;
    status)  cmd_status "$@" ;;
    logs)    cmd_logs "$@" ;;
    shell)   cmd_shell "$@" ;;
    backup)  cmd_backup "$@" ;;
    restore) cmd_restore "$@" ;;
    update)  cmd_update "$@" ;;
    clean)   cmd_clean "$@" ;;
    help|--help|-h) cmd_help ;;
    *)
        log_error "Unknown command: $1"
        cmd_help
        exit 1
        ;;
esac
