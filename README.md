# CloudClean AI (swachh-cloud) – Intelligent Cloud & Local Storage Optimization Platform
> **Enterprise Full-Stack Architecture: React 18 + Vite, Spring Boot 3.3.5 (Java 21), MongoDB 7.0 & Python FastAPI (OpenCV AI)**

CloudClean AI is a production-grade, full-stack SaaS platform engineered to analyze, detect, and safely optimize storage waste across local drives and cloud repositories (Google Drive). It leverages cryptographic deduplication (SHA-256), computer-vision heuristics (Laplacian variance, exposure diagnostics, perceptual dHash/aHash), user whitelisting, and a strict 9-step user-confirmed cleanup workflow.

---

## 🌟 Key Features & Capabilities

- **Intelligent Deduplication**: Exact cryptographic deduplication (SHA-256) with smart original selection and zero double-counting.
- **AI Image Quality Diagnostics**: Standalone Python FastAPI microservice utilizing OpenCV for variance of Laplacian blur detection, resolution auditing, luminance histogram evaluation, and perceptual image hashing (`dHash`/`aHash`).
- **Inactive & Large File Pruning**: Configurable threshold classification (30d, 90d, 180d, 365d) and multi-gigabyte bloat detection.
- **Smart Recommendation Engine**: Normalized junk scores (0–100), risk levels (`LOW`, `MEDIUM`, `HIGH`), and user protection whitelisting (`isProtected = true`).
- **Safe 9-Step Cleanup Center**: Enforces explicit modal confirmation (`confirmed: true`), computes exact pre-deletion impact, and moves files to provider trash (`.cloudclean_trash` directory sandbox / Google Drive Trash) rather than permanent deletion.
- **1-Click File Restoration**: Roll back any executed cleanup directly from the audit history table.
- **Zero Double-Counting Analytics**: Storage metrics computed strictly from distinct database records, ensuring overlapping signals never inflate recoverable space.
- **Streaming Reports & CSV Export**: Real-time storage distribution reports and RFC-4180 compliant CSV exports for file inventory and cleanup audit trails.
- **In-App Notification Center**: Real-time notification bell with unread counters and deep-linking for scan completions, alerts, and cleanups.
- **Enterprise Security**: HMAC SHA-256 JWT, AES-256 GCM cloud token encryption, rate limiting, and filesystem path traversal protection.

---

## 🏛️ System Architecture

```
cloudclean-ai/
├── backend/                       # Spring Boot 3.3.5 Modular Monolith (Java 21)
│   ├── Dockerfile                 # Multi-stage production container
│   ├── pom.xml                    # Maven build configuration
│   └── src/
│       ├── main/java/com/cloudclean/
│       │   ├── audit/             # AuditLog entity & security auditing service
│       │   ├── auth/              # AuthController, AuthService, UserDetails
│       │   ├── cleanup/           # CleanupService, CleanupHistory, Controller
│       │   ├── config/            # SecurityConfig, JwtTokenProvider, RateLimiterService
│       │   ├── dashboard/         # DashboardService (Zero Double-Count Engine)
│       │   ├── detection/         # Duplicate, Blurry, Large, Inactive detectors
│       │   ├── file/              # FileItem entity, FileService, Controller
│       │   ├── notification/      # Notification entity, Service, Controller
│       │   ├── recommendation/    # Recommendation entity, Service, Controller
│       │   ├── report/            # ReportService & RFC-4180 CSV export
│       │   ├── scanner/           # ScanJob entity, Async ScanService, Controller
│       │   ├── storage/           # StorageProvider abstraction, LocalStorageAdapter,
│       │   │                      # GoogleDriveAdapter, LocalAgentScanner
│       │   └── util/              # EncryptionUtils, FileHashUtils, ImageBlurDetector
│       └── test/                  # 54 comprehensive unit & integration tests
├── ai-service/                    # Python 3.11 FastAPI Computer Vision Microservice
│   ├── Dockerfile                 # Container with OpenCV Headless
│   ├── requirements.txt           # FastAPI, OpenCV, NumPy, Pillow, Pytest
│   ├── app/
│   │   ├── main.py                # REST endpoints (/analyze/blur, /analyze/full)
│   │   ├── blur_detector.py       # Variance of Laplacian blur detector
│   │   └── image_analyzer.py      # Resolution, exposure, dHash/aHash heuristics
│   └── test_ai_service.py         # 7 Pytest verification test cases
├── src/                           # React 18 Frontend with Vite & Tailwind CSS
│   ├── components/                # Modular UI kit, Charts, Modals, Navbar, FileTable
│   ├── context/                   # AuthContext, StorageContext
│   ├── pages/                     # 13 dedicated SaaS page views
│   ├── services/                  # apiClient, fileService, storageService, scanService
│   └── test/                      # 10 Vitest + React Testing Library suites
├── docker-compose.yml             # Orchestration for MongoDB, AI service, Backend, Frontend
├── Dockerfile                     # Multi-stage Nginx production container for React
├── nginx.conf                     # Nginx reverse proxy configuration
├── package.json                   # Frontend dependencies and scripts
└── vite.config.js                 # Vite bundler & Vitest configuration
```

---

## 🚀 Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | React 18, Vite 6, Tailwind CSS, React Router v7, Lucide Icons, Recharts, Axios, Vitest, Testing Library |
| **Core Backend** | Java 21 LTS, Spring Boot 3.3.5, Spring Security, Spring Data MongoDB, Spring Web, Springdoc OpenAPI 3 |
| **AI Microservice** | Python 3.11, FastAPI, OpenCV (`opencv-python-headless`), NumPy, Pillow, Pytest |
| **Database** | MongoDB 7.0 (with compound indexes, tenant isolation, and document auditing) |
| **DevOps & Cloud** | Docker, Docker Compose, Nginx, Google Drive API v3 (OAuth 2.0) |

---

## 🧪 Comprehensive Verification & Test Suite

The project includes thorough, multi-layer automated test coverage across all subsystems:

| Test Suite | Framework | Total Tests | Passed | Status |
| :--- | :--- | :---: | :---: | :---: |
| **Spring Boot Backend** | JUnit 5 + Mockito | 54 | 54 | **100% Passed** |
| **AI Computer Vision** | Pytest + NumPy | 7 | 7 | **100% Passed** |
| **React Frontend** | Vitest + Testing Library | 10 | 10 | **100% Passed** |
| **Vite Production Build** | Rollup / Vite | 2117 modules | 2117 | **Build Success** |
| **Maven Executable JAR** | Spring Boot Repackage | 1 artifact | 1 | **Packaged (`cloudclean-backend-1.0.0-SNAPSHOT.jar`)** |

---

## 🛠️ Quick Start & Execution Instructions

### Option 1: One-Click Docker Compose (Recommended)

```bash
# 1. Clone repository
git clone https://github.com/rockydr688-pixel/swachh-cloud.git
cd swachh-cloud

# 2. Setup environment configuration
cp .env.example .env

# 3. Launch all 4 services (MongoDB, AI Service, Backend, Frontend)
docker compose up --build -d

# 4. View live logs
docker compose logs -f
```

- **Frontend Dashboard**: `http://localhost:3000`
- **Backend Swagger UI**: `http://localhost:8000/swagger-ui.html`
- **AI Microservice Health**: `http://localhost:8001/health`

---

### Option 2: Running Locally for Development

#### 1. Start MongoDB
Ensure MongoDB is running locally on `localhost:27017` or update `MONGODB_URI` in `.env`.

#### 2. Start Python AI Microservice
```bash
cd ai-service
pip install -r requirements.txt
python -m pytest test_ai_service.py              # Run 7 tests
uvicorn app.main:app --host 127.0.0.1 --port 8001
```

#### 3. Start Spring Boot Backend
```bash
cd backend
mvn test                                         # Run 54 tests
mvn spring-boot:run
```
Backend starts on port **8000**. Default demo user is seeded automatically:
- **Email**: `rocky@cloudclean.ai`
- **Password**: `demopassword123`

#### 4. Start React Frontend
```bash
npm install
npm test                                         # Run 10 Vitest tests
npm run dev
```
Frontend starts on port **3000** (`http://localhost:3000`).

---

## 📖 Additional Documentation

- [ARCHITECTURE.md](file:///f:/gothilla/ARCHITECTURE.md): Detailed architectural design, service communication flow, database collections, and indexes.
- [API_DOCUMENTATION.md](file:///f:/gothilla/API_DOCUMENTATION.md): Complete REST endpoint catalog, request/response JSON schemas, and error codes.
- [DEPLOYMENT.md](file:///f:/gothilla/DEPLOYMENT.md): Containerization guide, production hardening checklist, and maintenance commands.

---

## 📄 License
This project is developed for educational evaluation, technical demonstration, and open-source publication under the MIT License.
