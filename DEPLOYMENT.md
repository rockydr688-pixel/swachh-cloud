# CloudClean AI – Deployment & Operations Guide

This guide covers deployment procedures for CloudClean AI across local development, containerized environments (Docker Compose), and production environments.

---

## 1. Prerequisites

Before deploying, ensure the host machine meets the following requirements:

| Tool | Minimum Version | Recommended Version |
| :--- | :--- | :--- |
| **Docker Engine** | 24.0+ | Latest Stable |
| **Docker Compose** | 2.20+ | Latest Compose v2 |
| **Node.js** (for bare-metal dev) | 18.x LTS | 20.x LTS |
| **Java JDK** (for bare-metal dev) | Java 21 LTS | Eclipse Temurin 21 |
| **Python** (for bare-metal dev) | 3.10+ | 3.11 or 3.12 |
| **MongoDB** | 6.0+ | 7.0 Community |

---

## 2. Docker Compose Deployment (Recommended)

The easiest and most reliable way to run the entire CloudClean AI stack is using Docker Compose.

### Step 1: Clone Repository & Prepare Environment
```bash
git clone https://github.com/rockydr688-pixel/swachh-cloud.git
cd swachh-cloud

# Copy template environment file
cp .env.example .env
```

### Step 2: Configure Environment Variables
Edit `.env` to supply secure secrets:
```dotenv
# Replace with a strong random 256-bit string (32+ chars)
JWT_SECRET=super_secret_production_jwt_signing_key_at_least_32_characters_long!

# Replace with a 32-character AES encryption key
ENCRYPTION_SECRET=super_secure_32_chars_aes_key!

# AI Microservice Internal Secret
AI_SERVICE_SECRET=internal_microservice_secret_token_32chars!

# Google Cloud OAuth (Optional if using only local scanning)
GOOGLE_CLIENT_ID=your_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URI=http://localhost:3000/oauth/callback
```

### Step 3: Build & Launch Containers
```bash
# Build images and start services in the background
docker compose up --build -d
```

### Step 4: Verify Running Services
```bash
docker compose ps
```

Expected output:
```text
NAME                     STATUS                   PORTS
cloudclean-mongodb       running (healthy)        0.0.0.0:27017->27017/tcp
cloudclean-ai-service    running (healthy)        0.0.0.0:8001->8001/tcp
cloudclean-backend       running                  0.0.0.0:8000->8000/tcp
cloudclean-frontend      running                  0.0.0.0:3000->80/tcp
```

### Step 5: Access the Platform
- **Frontend Dashboard**: Open `http://localhost:3000` in your browser.
- **Backend API & Swagger UI**: Open `http://localhost:8000/swagger-ui.html`.
- **AI Service Health**: Open `http://localhost:8001/health`.

---

## 3. Bare-Metal Development Setup (Without Docker)

If developing locally without Docker, run each component in separate terminal windows:

### 1. MongoDB
```bash
# Start local MongoDB daemon
mongod --dbpath ./data/db
```

### 2. Python AI Service
```bash
cd ai-service
python -m venv venv
# On Windows:
.\venv\Scripts\activate
# On Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload
```

### 3. Spring Boot Backend
```bash
cd backend
# Run test suite first
mvn test

# Start Spring Boot application
mvn spring-boot:run
```

### 4. React Frontend
```bash
# From repository root
npm install
npm run build
npm run dev
```

---

## 4. Production Security Hardening Checklist

- [x] **Disable Plaintext Secrets**: Ensure `.env` is listed in `.gitignore` and never committed.
- [x] **JWT Secret Strength**: Use an HMAC-SHA256 secret of 256 bits or higher.
- [x] **Database Credential Authentication**: In production, configure MongoDB authentication with strong credentials.
- [x] **HTTPS Termination**: Place Nginx or Traefik in front with a valid TLS certificate (e.g. Let's Encrypt).
- [x] **User Process Isolation**: Containers run with unprivileged users (`spring`, `appuser`, `nginx`).
- [x] **CORS Configuration**: Lock down `CORS_ALLOWED_ORIGINS` to the exact production domain.
- [x] **Safe Trash Relocation**: Local files are moved to `.cloudclean_trash` directory sandbox rather than permanent OS deletion.
- [x] **Explicit Confirmation Required**: Backend strictly requires `confirmed: true` before executing any cleanup operation.

---

## 5. Teardown & Maintenance

To stop services and preserve database data:
```bash
docker compose down
```

To stop services and remove volumes (clean wipe):
```bash
docker compose down -v
```

To view application logs in real-time:
```bash
docker compose logs -f backend
docker compose logs -f ai-service
docker compose logs -f frontend
```
