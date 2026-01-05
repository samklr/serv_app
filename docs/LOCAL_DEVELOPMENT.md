# Local Development Setup

This guide covers setting up and running Servantin locally for development.

## Quick Start (Docker Compose)

The fastest way to run the entire stack locally:

```bash
# Start all services (PostgreSQL, Backend, Frontend)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

**URLs:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## Manual Setup

### Prerequisites

| Tool | Version | Check Command |
|------|---------|---------------|
| Java JDK | 21 | `java --version` |
| Node.js | >= 20.x | `node --version` |
| PostgreSQL | 16 | `psql --version` |
| npm | >= 10.x | `npm --version` |

### 1. Database Setup

#### Using Docker (Recommended)

```bash
# Start PostgreSQL only
docker-compose up -d postgres

# Connection details:
# Host: localhost
# Port: 5432
# Database: servantin
# Username: postgres
# Password: postgres
```

#### Manual Installation

```bash
# macOS
brew install postgresql@16
brew services start postgresql@16

# Create database
createdb servantin
```

### 2. Backend Setup

```bash
cd backend

# Copy environment template
cp .env.example .env.local

# Edit configuration if needed
# (defaults work with docker-compose postgres)

# Run with Gradle
./gradlew bootRun

# Or build and run JAR
./gradlew bootJar
java -jar build/libs/servantin-api-0.0.1-SNAPSHOT.jar
```

**Backend will start on:** http://localhost:8080

**API Documentation:** http://localhost:8080/swagger-ui.html

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Create environment file
echo "NEXT_PUBLIC_API_URL=http://localhost:8080" > .env.local

# Run development server
npm run dev
```

**Frontend will start on:** http://localhost:3000

---

## Environment Variables

### Backend (`backend/.env.local`)

```properties
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/servantin
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres

# Security
JWT_SECRET=your-256-bit-secret-key-for-development-only
JWT_EXPIRATION=86400000

# Stripe (optional, use test keys)
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### Frontend (`frontend/.env.local`)

```properties
# API URL (points to local backend)
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## Test Users

The application seeds test users on startup in development mode:

| Email | Password | Role |
|-------|----------|------|
| admin@servantin.ch | Admin123! | ADMIN |
| provider@example.com | Provider123! | PROVIDER |
| client@example.com | Client123! | CLIENT |

---

## Development Workflow

### Running Tests

```bash
# Backend unit tests
cd backend
./gradlew test

# Backend with coverage
./gradlew test jacocoTestReport

# Frontend tests
cd frontend
npm test

# Frontend linting
npm run lint
```

### Database Migrations

Migrations run automatically on startup via Flyway.

```bash
# View migration status
./gradlew flywayInfo

# Run migrations manually
./gradlew flywayMigrate

# Create new migration
# Add file: src/main/resources/db/migration/V{VERSION}__{description}.sql
```

### Code Formatting

```bash
# Backend (uses default IDE formatting)
./gradlew spotlessApply  # if Spotless is configured

# Frontend
npm run lint -- --fix
```

### Hot Reload

Both frontend and backend support hot reload:

- **Frontend**: Changes to `.tsx`, `.ts`, `.css` files auto-reload
- **Backend**: DevTools enabled, changes require rebuild (`./gradlew build --continuous`)

---

## Common Development Tasks

### Create a New API Endpoint

1. **Create Controller** in `src/main/java/com/servantin/api/controller/`
2. **Create Service** in `src/main/java/com/servantin/api/service/`
3. **Create DTO** in `src/main/java/com/servantin/api/dto/`
4. **Add OpenAPI annotations** for documentation

### Add a New Frontend Page

1. **Create page** in `src/app/{route}/page.tsx`
2. **Add API call** in `src/lib/api.ts`
3. **Update navigation** if needed

### Add a Database Table

1. **Create migration** in `src/main/resources/db/migration/`
2. **Create Entity** in `src/main/java/com/servantin/api/entity/`
3. **Create Repository** in `src/main/java/com/servantin/api/repository/`

---

## Debugging

### Backend Debugging (IntelliJ IDEA)

1. Open `backend/` as a Gradle project
2. Create Run Configuration for `ServantinApplication`
3. Set breakpoints and run in Debug mode

### Backend Debugging (VS Code)

Add to `.vscode/launch.json`:

```json
{
  "type": "java",
  "name": "Debug Backend",
  "request": "launch",
  "mainClass": "com.servantin.api.ServantinApplication",
  "projectName": "servantin-api"
}
```

### Frontend Debugging

- Use browser DevTools (F12)
- React DevTools extension
- `console.log()` statements appear in browser console

### Database Inspection

```bash
# Connect to database
docker-compose exec postgres psql -U postgres -d servantin

# Common queries
\dt                    # List tables
\d users               # Describe users table
SELECT * FROM users;   # Query users
```

---

## Troubleshooting

### Port Already in Use

```bash
# Find process using port
lsof -i :8080  # or :3000

# Kill process
kill -9 <PID>
```

### Database Connection Failed

```bash
# Check if PostgreSQL is running
docker-compose ps

# Restart database
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

### Node Modules Issues

```bash
# Clear and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Gradle Issues

```bash
# Clear cache
./gradlew clean

# Refresh dependencies
./gradlew build --refresh-dependencies
```

---

## IDE Setup

### IntelliJ IDEA (Recommended for Backend)

1. Install plugins: Lombok, Spring Boot, Database Tools
2. Open as Gradle project
3. Enable annotation processing: Settings → Build → Compiler → Annotation Processors

### VS Code (Recommended for Frontend)

Extensions:
- ESLint
- Prettier
- Tailwind CSS IntelliSense
- ES7+ React/Redux/React-Native snippets

Settings (`.vscode/settings.json`):
```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  }
}
```

---

## Next Steps

Once local development is set up:

1. Read [DEPLOYMENT.md](./DEPLOYMENT.md) for production deployment
2. Review the API documentation at http://localhost:8080/swagger-ui.html
3. Check the component library in the frontend
