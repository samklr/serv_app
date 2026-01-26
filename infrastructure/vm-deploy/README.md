# Servantin VM Deployment

Deploy Servantin on a single Ubuntu VM with automatic HTTPS via Caddy.

**Live URL:** https://servapp.latticeiq.net

## Database Options

| Mode | Description | Best For |
|------|-------------|----------|
| **Local PostgreSQL** (default) | PostgreSQL runs as a Docker container | Full control, no external dependencies |
| **Supabase PostgreSQL** | Uses Supabase cloud database | Reduced VM resources, managed database |

See [SUPABASE_SETUP.md](SUPABASE_SETUP.md) for Supabase deployment details.

## Prerequisites

- Ubuntu 22.04 LTS (recommended) or 20.04 LTS
- Minimum 2GB RAM, 2 vCPUs, 20GB disk (1.5GB RAM with Supabase)
- Root or sudo access
- Domain DNS pointing to the VM IP (for automatic SSL)
- Ports 80 and 443 accessible

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/servantin.git
cd servantin/infrastructure/vm-deploy
```

### 2. Setup the VM

Run the setup script to install Docker, Docker Compose, and other tools:

```bash
sudo ./scripts/setup-vm.sh
```

This installs:
- Docker and Docker Compose
- Git, htop, curl, vim
- UFW firewall (ports 22, 80, 443)
- Fail2ban for SSH protection
- Swap file (2GB)
- System optimizations

After setup, log out and back in for Docker group changes to take effect.

### 3. Configure Environment

```bash
cp .env.example .env
nano .env
```

**Required settings:**
- `DOMAIN` - Your domain (default: servapp.latticeiq.net)
- `ACME_EMAIL` - Email for Let's Encrypt notifications
- `DB_PASSWORD` - Strong database password
- `JWT_SECRET` - Generate with: `openssl rand -base64 64`

**Optional settings:**
- AWS SES credentials for email
- GCP credentials for file storage
- Stripe credentials for payments

### 4. Deploy

```bash
./scripts/deploy.sh
```

The application will be available at:
- **Frontend:** https://servapp.latticeiq.net
- **API:** https://servapp.latticeiq.net/api
- **Swagger:** https://servapp.latticeiq.net/swagger-ui.html

SSL certificates are automatically provisioned by Caddy using Let's Encrypt.

### Alternative: Deploy with Supabase

To use Supabase PostgreSQL instead of local PostgreSQL:

```bash
# Copy Supabase environment template
cp .env.supabase.example .env
nano .env

# Deploy with Supabase flag
./scripts/deploy.sh --supabase
```

**Supabase required settings:**
- `SUPABASE_HOST` - Your Supabase project host (e.g., `abc123.supabase.co`)
- `SUPABASE_USER` - Database user (e.g., `postgres.abc123`)
- `SUPABASE_PASSWORD` - Your Supabase database password
- `SUPABASE_PORT` - Use `6543` for free tier (Transaction mode)

See [SUPABASE_SETUP.md](SUPABASE_SETUP.md) for detailed instructions.

## Management Commands

Use `./scripts/manage.sh` to manage the deployed application:

```bash
# Service management
./manage.sh start          # Start all services
./manage.sh stop           # Stop all services
./manage.sh restart        # Restart all services
./manage.sh restart caddy  # Restart specific service
./manage.sh status         # Show service status

# Logs
./manage.sh logs           # View last 100 lines
./manage.sh logs -f        # Follow logs (live)
./manage.sh logs -f backend  # Follow specific service

# Shell access
./manage.sh shell backend  # Open shell in backend container
./manage.sh shell postgres # Open PostgreSQL CLI
./manage.sh shell frontend # Open shell in frontend container
./manage.sh shell caddy    # Open shell in caddy container

# Database
./manage.sh backup         # Create database backup
./manage.sh restore        # List available backups
./manage.sh restore <file> # Restore from backup

# Updates
./manage.sh update         # Pull latest and redeploy

# Cleanup
./manage.sh clean          # Remove unused Docker resources
./manage.sh clean --volumes  # Also remove unused volumes
```

## Deployment Options

The deploy script accepts optional flags:

```bash
./scripts/deploy.sh --build     # Force rebuild images (no cache)
./scripts/deploy.sh --pull      # Pull latest code from git first
./scripts/deploy.sh --clean     # Clean up old images after deploy
./scripts/deploy.sh --supabase  # Use Supabase PostgreSQL instead of local
./scripts/deploy.sh --supabase --build --pull --clean  # All options
```

## SSL/HTTPS (Automatic)

Caddy automatically handles SSL certificates:

1. **Automatic provisioning** - Certificates are obtained from Let's Encrypt on first request
2. **Auto-renewal** - Certificates are renewed automatically before expiration
3. **HTTPS redirect** - HTTP requests are automatically redirected to HTTPS

### Requirements for Automatic SSL

- Domain DNS must point to the VM IP address
- Ports 80 and 443 must be accessible from the internet
- Valid email in `ACME_EMAIL` for Let's Encrypt notifications

### Testing with Staging Certificates

To avoid Let's Encrypt rate limits during testing, edit the Caddyfile:

```bash
nano /opt/servantin/config/caddy/Caddyfile
```

Uncomment the staging CA line:
```
acme_ca https://acme-staging-v02.api.letsencrypt.org/directory
```

Then restart Caddy:
```bash
./manage.sh restart caddy
```

## Architecture

```
                    ┌─────────────────────────────────────┐
                    │              Caddy                  │
                    │    (Reverse Proxy + Auto HTTPS)     │
                    │          Port 80/443                │
                    └──────────────┬──────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌────────────────┐
│      Frontend       │ │      Backend        │ │    Static      │
│     (Next.js)       │ │   (Spring Boot)     │ │    Assets      │
│     Port 3000       │ │     Port 8080       │ │                │
└─────────────────────┘ └──────────┬──────────┘ └────────────────┘
                                   │
                                   ▼
                       ┌─────────────────────┐
                       │     PostgreSQL      │
                       │     Port 5432       │
                       └─────────────────────┘
```

## File Structure

```
vm-deploy/
├── .env.example              # Environment template (local PostgreSQL)
├── .env.supabase.example     # Environment template (Supabase)
├── .env                      # Your configuration (create from template)
├── docker-compose.yml        # Docker Compose (local PostgreSQL)
├── docker-compose.supabase.yml  # Docker Compose (Supabase)
├── README.md                 # This file
├── SUPABASE_SETUP.md         # Supabase setup guide
├── config/
│   ├── caddy/
│   │   └── Caddyfile         # Caddy configuration (auto HTTPS)
│   └── postgres/
│       └── init.sql          # Database initialization
├── docker/
│   ├── Dockerfile.backend    # Backend build
│   └── Dockerfile.frontend   # Frontend build
└── scripts/
    ├── setup-vm.sh           # VM setup script
    ├── deploy.sh             # Deployment script
    └── manage.sh             # Management script
```

## Deployed Structure

After deployment, files are located at `/opt/servantin/`:

```
/opt/servantin/
├── .env                  # Configuration
├── docker-compose.yml    # Compose file
├── config/               # Configuration files
├── backups/              # Database backups
├── data/                 # Application data
└── logs/                 # Application logs
```

Caddy stores SSL certificates in a Docker volume (`caddy_data`).

## Resource Requirements

| Component  | Memory (min) | Memory (max) |
|------------|--------------|--------------|
| PostgreSQL | 256MB        | 512MB        |
| Backend    | 512MB        | 1GB          |
| Frontend   | 256MB        | 512MB        |
| Caddy      | 64MB         | 128MB        |
| **Total**  | **1GB**      | **2GB**      |

Recommended VM: 2GB+ RAM, 2 vCPUs, 20GB+ disk

## Troubleshooting

### SSL certificate not working

1. Verify DNS is pointing to VM:
```bash
dig servapp.latticeiq.net
```

2. Check Caddy logs:
```bash
./manage.sh logs -f caddy
```

3. Verify ports are open:
```bash
sudo ufw status
curl -I http://servapp.latticeiq.net
```

### Services not starting

Check logs:
```bash
./manage.sh logs
docker-compose -f /opt/servantin/docker-compose.yml logs --tail=50
```

### Database connection issues

Verify PostgreSQL is running:
```bash
docker-compose -f /opt/servantin/docker-compose.yml exec postgres pg_isready
```

Check connection:
```bash
./manage.sh shell postgres
```

### Backend health check failing

Check backend logs:
```bash
./manage.sh logs -f backend
```

Verify health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

### Frontend not loading

Check frontend logs:
```bash
./manage.sh logs -f frontend
```

Verify frontend is running:
```bash
curl http://localhost:3000
```

### Port conflicts

Check what's using the ports:
```bash
sudo lsof -i :80
sudo lsof -i :443
sudo lsof -i :8080
sudo lsof -i :3000
```

### Out of disk space

Clean up Docker resources:
```bash
./manage.sh clean
docker system prune -a
```

### Out of memory

Check memory usage:
```bash
free -h
./manage.sh status
```

Consider increasing VM RAM or reducing container limits.

## Security Notes

- Change default passwords in `.env`
- HTTPS is enabled by default with Caddy
- Keep system and Docker updated
- Review firewall rules
- Backups are stored in `/opt/servantin/backups/`
- Only last 7 backups are retained
- Actuator endpoints are blocked from external access

## Updating

To update to a new version:

```bash
cd /path/to/servantin/infrastructure/vm-deploy
git pull
./scripts/deploy.sh --pull --build --clean
```

Or use the management script:

```bash
./manage.sh update
```

This will:
1. Create a database backup
2. Pull latest code
3. Rebuild images
4. Restart services
