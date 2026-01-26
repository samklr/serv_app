# Supabase PostgreSQL Setup Guide

This guide explains how to deploy Servantin with **Supabase PostgreSQL** instead of a local PostgreSQL container.

## Why Supabase?

- **No local database management** - Supabase handles backups, updates, and maintenance
- **Reduced VM resources** - No PostgreSQL container means more RAM for your app
- **Free tier available** - 500MB storage, suitable for development and small deployments
- **Easy migration** - Can upgrade to paid plans as you scale

## 🧪 Demo Data & Accounts

The deployment includes demo data with the following test accounts (password for all: `password`):

| Role | Email | Name |
|------|-------|------|
| **Admin** | `admin@servantin.ch` | Admin User |
| **Provider** | `sarah@servantin.ch` | Sarah Nanny (Babysitting) |
| **Provider** | `mike@servantin.ch` | Mike Handyman (Home) |
| **Client** | `john@servantin.ch` | John Doe |
| **Client** | `jane@servantin.ch` | Jane Smith |

## ⚠️ Free Tier Limitations

| Limitation | Impact | Notes |
|------------|--------|-------|
| **Transaction mode only** (port 6543) | Must disable prepared statements | Already configured in `supabase` profile |
| **~60 active connections** | Keep connection pool small | Default: 5 connections |
| **500MB storage** | Monitor usage | Sufficient for most small apps |
| **Project pauses after 1 week inactivity** | DB goes offline | Set up health pings or use paid tier |
| **No session mode** | Some features limited | Works fine for most use cases |

---

## 🚀 Quick Start

### Step 1: Create Supabase Project

1. Go to [Supabase Dashboard](https://supabase.com/dashboard)
2. Click **New Project**
3. Fill in:
   - **Organization**: Select or create one
   - **Project name**: `servantin` (or your preferred name)
   - **Database password**: Choose a strong password (save this!)
   - **Region**: Choose closest to your VM
4. Click **Create new project**
5. Wait for project to be provisioned (~2 minutes)

### Step 2: Get Connection Details

1. In your Supabase project, go to **Project Settings** (gear icon)
2. Click **Database** in the sidebar
3. Find the **Connection string** section
4. Note down:
   - **Host**: `abc123xyz.supabase.co` (your project ref)
   - **Port**: `6543` (Transaction mode - required for free tier)
   - **Database**: `postgres`
   - **User**: `postgres.abc123xyz` (includes your project ref)
   - **Password**: The password you set during project creation

### Step 3: Configure Environment

On your VM:

```bash
cd /path/to/servantin/infrastructure/vm-deploy

# Copy Supabase environment template
cp .env.supabase.example .env

# Edit with your Supabase credentials
nano .env
```

Update these values:

```bash
# Your Supabase project host
SUPABASE_HOST=abc123xyz.supabase.co

# MUST be 6543 for free tier (Transaction mode)
SUPABASE_PORT=6543

# Database name (always 'postgres' for Supabase)
SUPABASE_DB=postgres

# User format: postgres.YOUR_PROJECT_REF
SUPABASE_USER=postgres.abc123xyz

# Your database password
SUPABASE_PASSWORD=your-strong-password-here

# Generate JWT secret
JWT_SECRET=$(openssl rand -base64 64)

# Your domain
DOMAIN=your-domain.com
ACME_EMAIL=admin@your-domain.com
```

### Step 4: Deploy

```bash
# Deploy with Supabase mode
./scripts/deploy.sh --supabase --build
```

The deployment script will:
1. Validate your Supabase configuration
2. Test connectivity (if psql is available)
3. Build and start the application without a local PostgreSQL container
4. Run Flyway migrations on first startup

---

## 🔧 Technical Details

### Connection String Format

The application connects using:

```
jdbc:postgresql://<HOST>:6543/postgres?prepareThreshold=0&ssl=require&sslmode=require
```

Key parameters:
- `prepareThreshold=0`: **CRITICAL** - Disables prepared statements for PgBouncer compatibility
- `ssl=require`: Enforces SSL connection
- `sslmode=require`: SSL mode setting

### Spring Profile

The `supabase` profile in `application.yml` configures:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5  # Keep low for free tier
      minimum-idle: 1
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 0     # Disabled for PgBouncer
        order_inserts: false
        order_updates: false
```

### Why These Settings?

**PgBouncer Transaction Mode** shares connections between clients, which means:

1. **No prepared statements** - Each transaction gets a potentially different connection
2. **No statement caching** - Can't cache across connection changes
3. **No batching** - Batch operations require connection stickiness

---

## 📋 Management Commands

All `manage.sh` commands work with Supabase. The script auto-detects Supabase mode:

```bash
# View status (shows database mode)
./manage.sh status

# View logs
./manage.sh logs -f backend

# Access PostgreSQL shell (requires psql installed)
./manage.sh shell postgres

# Create backup (from Supabase to local file)
./manage.sh backup

# Restore backup (from local file to Supabase)
./manage.sh restore servantin_20250125_120000.sql.gz
```

### Installing psql Client

For backup/restore and shell access, install PostgreSQL client:

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install postgresql-client

# Verify installation
psql --version
```

---

## 🔐 Security Considerations

### Connection Security

- All connections use SSL (enforced in connection string)
- Supabase provides automatic certificate management
- IP restrictions available in Supabase Dashboard (paid plans)

### Credential Management

- Never commit `.env` file to version control
- Use strong database passwords
- Rotate passwords periodically via Supabase Dashboard

### Network Access

Supabase free tier is publicly accessible. For enhanced security:
1. Use strong passwords
2. Consider upgrading to paid tier for VPC/IP restrictions
3. Monitor access logs in Supabase Dashboard

---

## 🔄 Migrating from Local PostgreSQL

If you have an existing deployment with local PostgreSQL:

### Step 1: Backup Local Database

```bash
# Create backup from local PostgreSQL
./manage.sh backup
```

### Step 2: Create Supabase Project

Follow the Quick Start steps above.

### Step 3: Update Configuration

```bash
# Copy Supabase template
cp .env.supabase.example .env.new

# Merge your existing JWT_SECRET and other settings
# Then replace .env
mv .env.new .env
```

### Step 4: Restore to Supabase

```bash
# Deploy with Supabase (this will run migrations)
./scripts/deploy.sh --supabase --build

# Restore your data
./manage.sh restore /opt/servantin/backups/servantin_YYYYMMDD_HHMMSS.sql.gz
```

---

## 🐛 Troubleshooting

### "Connection refused" or timeout

1. Check your `SUPABASE_HOST` is correct
2. Verify port is `6543` (not `5432`)
3. Check Supabase project is active (not paused)

```bash
# Test connectivity
PGPASSWORD="your-password" psql \
  -h abc123xyz.supabase.co \
  -p 6543 \
  -U postgres.abc123xyz \
  -d postgres \
  -c "SELECT 1;"
```

### "Prepared statement already exists" errors

This indicates `prepareThreshold=0` is not set. Verify your Spring profile:

```bash
# Check backend environment
docker exec servantin-api env | grep SPRING
# Should show: SPRING_PROFILES_ACTIVE=supabase
```

### Backend fails to start

Check logs for database connection issues:

```bash
./manage.sh logs -f backend
```

Common issues:
- Wrong password
- Project paused (free tier)
- Wrong port (must be 6543)

### Project paused (free tier)

Supabase pauses projects after 1 week of inactivity:

1. Go to Supabase Dashboard
2. Select your project
3. Click "Resume project"
4. Wait ~2 minutes for it to be ready

**Prevent pausing**: Set up a cron job to ping the health endpoint:

```bash
# Add to crontab (every 6 days)
0 0 */6 * * curl -sf https://your-domain.com/health > /dev/null
```

---

## 📊 Monitoring

### Supabase Dashboard

Monitor your database from the Supabase Dashboard:
- **Database** → View tables and run queries
- **Reports** → Database size and connection stats
- **Logs** → Query logs and error logs

### Application Metrics

```bash
# Check backend health
curl -sf http://localhost:8080/actuator/health | jq

# Check application status
./manage.sh status
```

---

## 📚 Additional Resources

- [Supabase Documentation](https://supabase.com/docs)
- [Supabase Connection Pooling](https://supabase.com/docs/guides/database/connecting-to-postgres#connection-pooling)
- [PgBouncer Transaction Mode](https://www.pgbouncer.org/features.html)
- [Spring Boot with PgBouncer](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html)
