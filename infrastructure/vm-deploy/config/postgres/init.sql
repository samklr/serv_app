-- Servantin Database Initialization Script
-- This script runs on first database initialization

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS public;

-- Grant privileges (for production, create a specific app user)
-- The main user is already created by POSTGRES_USER env var

-- Performance tuning (optional, can also be set in postgresql.conf)
-- These are reasonable defaults for a demo VM with 2-4GB RAM

-- Log configuration for debugging (remove in production)
-- ALTER SYSTEM SET log_statement = 'all';
-- ALTER SYSTEM SET log_min_duration_statement = 1000;

-- Connection settings
-- ALTER SYSTEM SET max_connections = 100;

-- Memory settings (adjust based on VM size)
-- ALTER SYSTEM SET shared_buffers = '256MB';
-- ALTER SYSTEM SET effective_cache_size = '768MB';
-- ALTER SYSTEM SET maintenance_work_mem = '64MB';
-- ALTER SYSTEM SET work_mem = '4MB';

-- Write-ahead log settings
-- ALTER SYSTEM SET wal_buffers = '8MB';
-- ALTER SYSTEM SET checkpoint_completion_target = 0.9;

-- Planner settings
-- ALTER SYSTEM SET random_page_cost = 1.1;
-- ALTER SYSTEM SET effective_io_concurrency = 200;

-- Reload configuration (if ALTER SYSTEM statements are uncommented)
-- SELECT pg_reload_conf();

-- Create read-only user for reporting (optional)
-- CREATE USER servantin_readonly WITH PASSWORD 'change_this_password';
-- GRANT CONNECT ON DATABASE servantin TO servantin_readonly;
-- GRANT USAGE ON SCHEMA public TO servantin_readonly;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO servantin_readonly;
