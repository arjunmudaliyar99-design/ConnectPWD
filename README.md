# ConnectPWD

**India's first voice-enabled, bilingual, chat-style functional assessment platform for persons with disabilities (PWD).**

ConnectPWD digitises the Indian Scale for Assessment of Autism (ISAA) — a standardised tool published by the National Institute for the Mentally Handicapped (NIMH) — into a mobile-first, conversational interface with bilingual support (English + Hindi) and Web Speech API integration.

---

## Architecture

| Layer | Technology |
|-------|-----------|
| Frontend | React 18 + Vite 5 + Zustand + CSS Modules |
| Backend | Java 17 + Spring Boot 3.2.5 + Spring Security 6 |
| Databases | PostgreSQL 15, MongoDB 7, Redis 7 |
| Storage | Cloudflare R2 (S3-compatible) |
| PDF | iText 7.2.5 |
| Auth | JWT (HS256) — 15min access / 7-day refresh |
| Deploy | Docker Compose (dev), Railway + Vercel (prod) |

## Project Structure

```
ConnectPWD/
├── backend/
│   ├── src/main/java/org/connectpwd/
│   │   ├── auth/          # JWT auth, register/login
│   │   ├── user/          # User entity & service
│   │   ├── consent/       # Informed consent
│   │   ├── question/      # Question bank (4 levels, bilingual JSON)
│   │   ├── session/       # Assessment session management
│   │   ├── answer/        # Response collection (MongoDB)
│   │   ├── scoring/       # ISAA scoring engine (NIMH table)
│   │   ├── storage/       # Cloudflare R2 integration
│   │   ├── report/        # PDF report generation
│   │   ├── config/        # Security, CORS, Redis, Mongo, Rate limiting
│   │   └── common/        # Shared exceptions, API envelope, audit
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── questions/     # level1-4 JSON files
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/           # Axios + JWT interceptor
│   │   ├── store/         # Zustand (auth, session)
│   │   ├── hooks/         # Speech I/O, voice recorder, offline queue
│   │   ├── pages/         # Landing, Consent, Assessment
│   │   └── components/    # Chat UI, Scale/Chip inputs, TopBar, etc.
│   ├── public/            # PWA manifest, service worker
│   └── package.json
├── nginx/                 # Production reverse proxy config
├── .github/workflows/     # CI + Deploy pipelines
├── docker-compose.yml     # Local dev
├── docker-compose.prod.yml
└── .env.example
```

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 17+ (for local dev without Docker)
- Node.js 20+ (for frontend dev)

### 1. Clone & configure

```bash
git clone https://github.com/your-org/ConnectPWD.git
cd ConnectPWD
cp .env.example .env
# Fill in JWT_SECRET, R2 credentials, etc.
```

### 2. Run with Docker Compose

```bash
docker compose up -d
```

- Backend: http://localhost:8080
- Frontend: http://localhost:5173

### 3. Local development (without Docker)

**Backend:**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Refresh tokens |
| POST | `/api/v1/consent` | Submit informed consent |
| POST | `/api/v1/session/start` | Start assessment session |
| GET | `/api/v1/session/{id}` | Get session status + current question |
| POST | `/api/v1/answer/text` | Submit text/scale answer |
| POST | `/api/v1/answer/voice` | Submit voice recording |
| POST | `/api/v1/session/{id}/score` | Compute ISAA score |
| GET | `/api/v1/session/{id}/score` | Get ISAA score |
| POST | `/api/v1/session/{id}/report` | Generate PDF report |
| GET | `/api/v1/session/{id}/report` | Get report download link |

## Assessment Levels

| Level | Questions | Type | Description |
|-------|-----------|------|-------------|
| 1 | 10 | Intake | Basic info, medical history, concerns |
| 2 | 40 | ISAA | Official 40-item ISAA (6 domains, scale 1-5) |
| 3 | 10 | Therapy | Communication, therapy recommendations, daily living |
| 4 | 9 | Vocational | Pre-vocational skills, interests, community skills |

## ISAA Scoring

Scoring follows the exact NIMH ISAA Manual:

| Total Score | Severity | Disability % |
|------------|----------|-------------|
| <70 | No Autism | 0% |
| 70 | Mild | 40% |
| 71–88 | Mild | 50% |
| 89–105 | Mild | 60% |
| 106–123 | Moderate | 70% |
| 124–140 | Moderate | 80% |
| 141–158 | Severe | 90% |
| >158 | Severe | 100% |

## Security

- BCrypt (strength 12) password hashing
- JWT with HS256 signing
- CORS restriction with configurable origins
- HSTS, CSP, X-Frame-Options security headers
- Redis-backed rate limiting (100 req/min general, 10 req/min auth)
- @PreAuthorize role-based access control

## Testing

**Backend:**
```bash
cd backend
mvn test
```

**Frontend:**
```bash
cd frontend
npm test
```

## Deployment

- **Backend:** Railway (via GitHub Actions)
- **Frontend:** Vercel (via GitHub Actions)
- **CI:** Runs on every push/PR to `main`

## License

MIT