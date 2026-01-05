# Servantin - Service Marketplace

A modern service marketplace connecting clients with qualified service providers in Switzerland.

## 🚀 Quick Links

| Link | Description |
|------|-------------|
| [Local Development](docs/LOCAL_DEVELOPMENT.md) | Set up your local environment |
| [Deployment Guide](docs/DEPLOYMENT.md) | Deploy to GCP Cloud Run |
| [Infrastructure](infrastructure/README.md) | Terraform configuration |
| [API Docs](http://localhost:8080/swagger-ui.html) | OpenAPI documentation (local) |

---

## 📋 Overview

Servantin is a full-stack web application that enables:

- **Clients** to find and book qualified service providers
- **Providers** to manage their profiles, availability, and bookings
- **Admins** to oversee the platform and verify providers

### Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS |
| Backend | Spring Boot 3.2, Java 21, PostgreSQL 16 |
| Infrastructure | GCP Cloud Run, Cloud SQL, Terraform |
| Authentication | JWT with Spring Security |
| Payments | Stripe (planned) |

---

## 🏗️ Project Structure

```
servantin/
├── backend/                 # Spring Boot API
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration & migrations
│   └── Dockerfile          # Container build
│
├── frontend/               # Next.js application
│   ├── src/app/           # App router pages
│   ├── src/components/    # React components
│   ├── src/lib/           # Utilities & API client
│   └── Dockerfile         # Container build
│
├── infrastructure/         # Terraform IaC
│   └── terraform/
│       ├── main.tf        # Resource definitions
│       ├── variables.tf   # Input variables
│       ├── outputs.tf     # Output values
│       └── environments/  # Environment configs
│
├── scripts/               # Deployment scripts
│   ├── setup-gcp.sh      # GCP project setup
│   └── deploy.sh         # Deployment automation
│
├── docs/                  # Documentation
│   ├── DEPLOYMENT.md     # Production deployment
│   └── LOCAL_DEVELOPMENT.md
│
├── docker-compose.yml     # Local development stack
└── cloudbuild.yaml        # GCP Cloud Build config
```

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Node.js 20+
- Java 21
- Terraform 1.5+ (for deployment)

### Local Development

```bash
# Clone the repository
git clone https://github.com/your-org/servantin.git
cd servantin

# Start all services with Docker Compose
docker-compose up -d

# Access the application
open http://localhost:3000
```

See [Local Development Guide](docs/LOCAL_DEVELOPMENT.md) for detailed setup instructions.

### Production Deployment

```bash
# Set up GCP project
./scripts/setup-gcp.sh YOUR_PROJECT_ID

# Deploy to development
./scripts/deploy.sh dev
```

See [Deployment Guide](docs/DEPLOYMENT.md) for complete deployment documentation.

---

## 🔐 Test Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@servantin.ch | Admin123! |
| Provider | provider@example.com | Provider123! |
| Client | client@example.com | Client123! |

---

## 📚 API Documentation

When running locally, access the API documentation at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

---

## 🧪 Running Tests

```bash
# Backend tests
cd backend && ./gradlew test

# Frontend tests
cd frontend && npm test

# Frontend linting
cd frontend && npm run lint
```

---

## 🏛️ Architecture

### High-Level Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│   Next.js SSR   │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│   (Frontend)    │     │  (Backend API)  │     │   (Database)    │
│                 │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │                       │
        ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│  Cloud Storage  │     │ Secret Manager  │
│   (Uploads)     │     │   (Secrets)     │
└─────────────────┘     └─────────────────┘
```

### Key Design Decisions

1. **Next.js with SSR**: SEO-friendly, fast initial loads
2. **API Proxy**: Frontend proxies API requests to avoid CORS
3. **JWT Authentication**: Stateless, scalable auth
4. **Cloud SQL Private IP**: Secure database access
5. **Terraform IaC**: Reproducible, version-controlled infrastructure

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- **Backend**: Follow standard Java conventions
- **Frontend**: ESLint + Prettier (auto-formatted on save)
- **Terraform**: `terraform fmt` before committing

---

## 📄 License

This project is proprietary. All rights reserved.

---

## 📞 Support

For questions or issues:
- Open a GitHub Issue
- Contact the development team
