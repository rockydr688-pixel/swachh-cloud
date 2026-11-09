# CloudClean AI (swachh-cloud) – Intelligent Cloud Storage Optimization Platform
> **Enterprise Full-Stack Architecture: React 18 + Vite & Spring Boot 3.3 + MongoDB**

CloudClean AI is a production-grade SaaS platform designed to analyze, detect, and optimize storage waste across local filesystems and cloud repositories using cryptographic deduplication and computer-vision heuristics.

---

## 🏛️ System Architecture

```
cloudclean-ai/
├── backend/                       # Spring Boot 3.x Modular Monolith Backend
│   ├── pom.xml                    # Maven configuration (Java 21, Spring Boot 3.3.5)
│   └── src/
│       ├── main/java/com/cloudclean/
│       │   ├── config/            # SecurityConfig, JWT Provider, Filter, Async, OpenAPI
│       │   ├── auth/              # AuthController, AuthService, UserDetailsServiceImpl
│       │   ├── user/              # User model, UserService
│       │   ├── storage/           # StorageConnection model, Service, Controller
│       │   ├── scanner/           # ScanJob model, Async ScanService, ScanController
│       │   ├── file/              # FileItem model, FileService, FileController
│       │   ├── detection/         # DuplicateGroup model, DuplicateDetectionService
│       │   ├── analysis/          # FileAnalysis model, ImageAnalysisService
│       │   ├── recommendation/    # Recommendation model, Service, Controller
│       │   ├── cleanup/           # CleanupHistory model, CleanupService, Controller
│       │   ├── dashboard/         # DashboardService, DashboardController
│       │   ├── repository/        # MongoDB Repositories (Users, Files, Scans, etc.)
│       │   ├── dto/               # Standardized API response envelopes & DTOs
│       │   ├── exception/         # GlobalExceptionHandler & custom exceptions
│       │   └── util/              # SecurityUtils, EncryptionUtils, FileHashUtils, ImageBlurDetector
│       └── main/resources/
│           └── application.yml    # MongoDB URI, JWT secret, CORS, Swagger config
├── src/                           # React 18 Frontend
│   ├── components/                # Layout, common UI kit, charts, and file management
│   ├── context/                   # AuthContext, StorageContext, ToastContext
│   ├── pages/                     # 13 dedicated SaaS page views
│   └── services/                  # api.js, fileService.js, storageService.js, scanService.js
├── tailwind.config.js
├── vite.config.js
└── package.json
```

---

## 🚀 Technology Stack

### Backend
- **Language & Runtime**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.5
- **Modules**:
  - Spring Web (RESTful APIs)
  - Spring Security (JWT authentication filter & BCrypt password encoder)
  - Spring Data MongoDB (Collections, compound indexes, auditing)
  - Spring Validation (Jakarta constraints)
  - Springdoc OpenAPI (Swagger 3 UI at `/swagger-ui.html`)
  - JJWT 0.12.6 (HMAC-SHA256 bearer tokens)
- **Database**: MongoDB (Atlas or local `mongodb://localhost:27017/cloudclean`)

### Frontend
- **Framework**: React.js 18 with Vite
- **Styling**: Tailwind CSS with custom design system tokens
- **Routing**: React Router v7 (`react-router-dom`)
- **Icons**: Lucide React
- **Analytics & Charts**: Recharts
- **HTTP Client**: Axios with configured request/response interceptors

---

## 🗄️ MongoDB Collections & Models

1. **`users`** (`User.java`): `@Id`, unique indexed `email`, `passwordHash` (BCrypt), `fullName`, `role` (`ROLE_USER`, `ROLE_ADMIN`), timestamps.
2. **`storage_connections`** (`StorageConnection.java`): User ownership index, adapter type (`local`, `google_drive`), AES-encrypted OAuth tokens, monitored folder paths, quota usage.
3. **`files`** (`FileItem.java`): Compound indexes `[userId, category]`, `[userId, hash]`, sanitization of host paths, SHA-256 hash, Laplacian blur score, recommendation status.
4. **`scan_jobs`** (`ScanJob.java`): Asynchronous job execution tracking, progress percentage (0-100%), duration, and savings calculation.
5. **`duplicate_groups`** (`DuplicateGroup.java`): Exact cryptographic hash clusters, canonical original pointer, and redundant copy IDs.
6. **`file_analyses`** (`FileAnalysis.java`): Diagnostic metrics (Laplacian edge variance, motion blur, and confidence scores).
7. **`recommendations`** (`Recommendation.java`): Prioritized actionable recipes with safety confidence scores (e.g. 98% safe for exact clones).
8. **`cleanup_history`** (`CleanupHistory.java`): Cryptographically verified audit log of executed actions, operator identity, method, and freed space.

---

## 📡 REST API Reference

All endpoints return a standardized `ApiResponse<T>` envelope:
`{ "success": true, "message": "...", "data": T, "timestamp": "...", "status": 200 }`

### Authentication (`/api/auth`)
- `POST /api/auth/register` – Register user with encrypted BCrypt password
- `POST /api/auth/login` – Authenticate credentials and receive JWT Bearer token
- `GET /api/auth/me` – Inspect current authenticated user identity

### Storage Connections (`/api/storage`)
- `GET /api/storage` – List connected local agent & cloud drives
- `POST /api/storage/local/register` – Register local monitored folder paths
- `POST /api/storage/google/connect` – Encrypt and save Google Drive OAuth tokens
- `DELETE /api/storage/{id}` – Disconnect storage adapter

### Scanning & Diagnostics (`/api/scans`)
- `POST /api/scans` – Launch asynchronous multi-threaded scan job
- `GET /api/scans` – Get user's paginated scan history
- `GET /api/scans/{id}` – Poll real-time progress and diagnostics
- `POST /api/scans/{id}/cancel` – Cancel active in-progress scan

### File Management (`/api/files`)
- `GET /api/files` – Paginated files with category filtering (`duplicate`, `blurry`, `large`, `inactive`) and sorting
- `GET /api/files/search` – Search files across names, locations, and detection reasons
- `GET /api/files/{id}` – Retrieve file diagnostics and metadata
- `DELETE /api/files/{id}` – Soft delete file to recovery trash pool

### Cleanup & Recommendations (`/api/recommendations`, `/api/cleanup`)
- `GET /api/recommendations` – Ranked AI cleanup advice
- `PATCH /api/recommendations/{id}` – Update recommendation status (`applied`, `dismissed`)
- `POST /api/cleanup` – Execute cleanup (**strictly requires** `confirmed: true`)
- `GET /api/cleanup/history` – Retrieve audited cleanup execution history

### Dashboard (`/api/dashboard`)
- `GET /api/dashboard` – Aggregated storage capacity, recoverable bloat, and analytics

---

## 🔐 Security & Governance Standard

1. **User Ownership**: Every repository query validates `userId` against the authenticated JWT principal.
2. **Zero Plaintext Passwords**: Password verification uses BCrypt with cost factor 10.
3. **Encrypted Cloud Credentials**: Google Drive OAuth tokens are encrypted at rest using AES-256-GCM (`EncryptionUtils.java`).
4. **Path Sanitization**: Operating system root paths (`C:/Users/...`, `/home/...`) are sanitized to `User:/` in responses to prevent filesystem enumeration.
5. **Mandatory Confirmation**: Cleanup operations reject execution if `confirmed: true` is not explicitly declared.
6. **Stateless JWT**: All protected endpoints validate Bearer tokens on each HTTP request via `JwtAuthenticationFilter`.

---

## 🛠️ Instructions to Run

### Prerequisites
- Java 21 LTS
- Maven 3.8+
- Node.js 18+ & npm
- MongoDB (local on port 27017 or a MongoDB Atlas connection string)

### 1. Configure MongoDB Connection (Optional)
By default, the backend connects to `mongodb://localhost:27017/cloudclean`.
To use MongoDB Atlas or custom URI, set the environment variable:
```bash
export MONGODB_URI="mongodb+srv://<username>:<password>@cluster0.mongodb.net/cloudclean"
# Or on Windows PowerShell:
$env:MONGODB_URI="mongodb+srv://<username>:<password>@cluster0.mongodb.net/cloudclean"
```

### 2. Run the Spring Boot Backend
```bash
cd backend
mvn spring-boot:run
```
The backend starts on port **8000**.
- Swagger UI Documentation: `http://localhost:8000/swagger-ui.html`
- OpenAPI Specification: `http://localhost:8000/v3/api-docs`

Default Admin Account seeded automatically:
- **Email**: `rocky@cloudclean.ai`
- **Password**: `demopassword123`

### 3. Run the Frontend
In a separate terminal:
```bash
npm install
npm run dev
```
Open `http://localhost:3000` in your browser. You can click **"Demo Auto-Login"** or sign in with your registered credentials.
