# CloudClean AI – System Architecture Documentation

CloudClean AI is a microservice-augmented modular monolith designed for intelligent, enterprise-grade cloud and local storage optimization. The architecture prioritizes data safety, tenant isolation, zero double-counting in storage diagnostics, and multi-signal AI analysis.

---

## 1. High-Level Architecture Diagram

```mermaid
flowchart TD
    subgraph Client["Frontend Layer (React 18 + Vite)"]
        UI["React SPA Dashboard (Tailwind CSS)"]
        AuthUI["JWT Auth & State Store"]
        CleanUI["Cleanup Center & Confirmation Modal"]
    end

    subgraph Gateway["Reverse Proxy / Ingress"]
        Nginx["Nginx Reverse Proxy (:3000 -> :80)"]
    end

    subgraph Backend["Core Backend (Spring Boot 3.3.5 / Java 21)"]
        Sec["Spring Security & JWT Filter"]
        Rate["Token Bucket Rate Limiter"]
        Audit["Audit Logging Service"]

        subgraph CoreServices["Domain Services"]
            AuthSvc["AuthService & UserDetailsService"]
            StorageSvc["StorageService & Connection Registry"]
            ScanSvc["ScanService (Async Thread Pool)"]
            FileSvc["FileService & Metadata Registry"]
            DupSvc["DuplicateDetectionService (SHA-256)"]
            RecSvc["RecommendationService (Junk Scoring)"]
            CleanSvc["CleanupService (9-Step Trash Pipeline)"]
            DashSvc["DashboardService (Zero Double-Count)"]
            RepSvc["ReportService & CSV Streaming"]
            NotifSvc["NotificationService"]
        end

        subgraph StorageAdapters["Storage Provider Layer"]
            LocalAdp["LocalStorageAdapter (Recycle Bin / Trash Sandbox)"]
            GDriveAdp["GoogleDriveAdapter (Drive API v3 / OAuth2)"]
            LocalAgent["LocalAgentScanner (Whitelisted FS Traversal)"]
        end
    end

    subgraph AIService["AI Microservice (FastAPI / Python 3.11)"]
        BlurDet["BlurDetector (Laplacian Variance)"]
        ImgAna["ImageAnalyzer (Resolution, Exposure, dHash/aHash)"]
    end

    subgraph Database["Data Persistence"]
        Mongo[("MongoDB 7.0 (Collections & Compound Indexes)")]
    end

    subgraph StorageSources["Target Storage Repositories"]
        LocalFS["Local File System (Permitted Folders)"]
        GoogleCloud["Google Drive (Cloud Storage)"]
    end

    UI -->|HTTPS / Axios| Nginx
    Nginx -->|Proxy /api| Sec
    Sec --> Rate
    Rate --> CoreServices
    CoreServices --> Audit
    CoreServices --> Mongo

    ScanSvc --> StorageAdapters
    StorageAdapters --> LocalFS
    StorageAdapters --> GoogleCloud

    FileSvc --> AIService
    CleanSvc --> StorageAdapters
    AIService --> Mongo
```

---

## 2. Inter-Service Communication Flow

### A. Scanning & Detection Pipeline
1. **Trigger Scan**: User requests a scan on a configured target (`Local Computer` or `Google Drive`).
2. **Asynchronous Execution**: `ScanService` enqueues a `ScanJob` (`@Async` thread pool), returning `202 Accepted` with a `scanId`.
3. **Provider Traversal**:
   - For **Local Storage**: `LocalAgentScanner` traverses only user-approved directories. Restricted paths (`C:\Windows`, `/etc`, etc.) and directory traversals (`..`) are blocked. SHA-256 checksums are calculated streamingly.
   - For **Google Drive**: `GoogleDriveAdapter` fetches file metadata via Google Drive API v3 using user OAuth access tokens (auto-refreshed via encrypted refresh tokens).
4. **AI Image Analysis**:
   - Image files are submitted via internal HTTP to the **FastAPI AI Microservice** (`/analyze/full`).
   - The microservice executes OpenCV Laplacian variance, calculates perceptual difference hashes (`dHash`, `ahash`), and detects under/overexposure without storing raw images.
5. **Deduplication & Junk Scoring**:
   - `DuplicateDetectionService` clusters files by exact SHA-256 and size matching.
   - `RecommendationService` applies multi-signal weights (`junkScore` 0–100) and risk levels (`LOW`, `MEDIUM`, `HIGH`).
6. **Notification**: `NotificationService` emits an in-app alert upon scan completion.

### B. Safe Cleanup & Recovery Pipeline
1. **Candidate Selection**: User selects candidate files in `CleanupRecommendations.jsx`.
2. **Impact Calculation**: Backend calculates exact recoverable space via `/api/cleanup/impact`, filtering out protected whitelisted files.
3. **Explicit Confirmation**: The user must check the explicit confirmation checkbox (`confirmed: true`); unconfirmed requests fail with `400 Bad Request`.
4. **Provider-Safe Deletion**:
   - Files are moved to provider trash (`.cloudclean_trash` directory sandbox for local; Drive Trash API for cloud). Files are never permanently purged without explicit secondary intent.
5. **Reconciliation & Audit**:
   - Storage reclaimed is only recorded once confirmed by the provider adapter.
   - Detailed snapshot is saved to `cleanup_history`.
   - Restoration is available anytime via `/api/cleanup/{id}/restore`.

---

## 3. Database Design & MongoDB Collections

All entities reside in the `cloudclean` database with index optimizations:

### 1. `users`
- Stores user credentials and role claims.
- **Indexes**:
  - `email` (Unique)
  - `createdAt`

### 2. `storage_connections`
- Monitored drive configurations and OAuth credentials.
- **Fields**: `userId`, `provider` (`LOCAL`, `GOOGLE_DRIVE`), `encryptedAccessToken`, `encryptedRefreshToken`, `monitoredPaths`, `connectedAt`.
- **Indexes**:
  - Compound: `{ userId: 1, provider: 1 }`

### 3. `files`
- Comprehensive file catalog across all storage sources.
- **Fields**: `userId`, `storageConnectionId`, `fileName`, `fileExtension`, `fileSize`, `mimeType`, `storageLocation`, `sha256Hash`, `isDuplicate`, `isProtected`, `isExcluded`, `isTrashed`.
- **Indexes**:
  - Compound: `{ userId: 1, sha256Hash: 1 }`
  - Compound: `{ userId: 1, isDuplicate: 1 }`
  - Compound: `{ userId: 1, isProtected: 1 }`
  - Single: `storageLocation`

### 4. `scan_jobs`
- Real-time and historical scan telemetry.
- **Fields**: `userId`, `targetStorage`, `status` (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `CANCELLED`), `filesScanned`, `bytesScanned`, `startedAt`, `completedAt`.
- **Indexes**:
  - Compound: `{ userId: 1, createdAt: -1 }`

### 5. `duplicate_groups`
- Exact cryptographic hash collision clusters.
- **Fields**: `userId`, `sha256Hash`, `fileSize`, `originalFileId`, `duplicateFileIds`, `potentialSavingsBytes`.
- **Indexes**:
  - Compound: `{ userId: 1, sha256Hash: 1 }`

### 6. `file_analyses`
- AI computer-vision image telemetry.
- **Fields**: `fileId`, `userId`, `laplacianVariance`, `blurScore`, `isBlurry`, `dhash`, `exposureStatus`, `qualityScore`.
- **Indexes**:
  - Single: `fileId` (Unique)
  - Compound: `{ userId: 1, isBlurry: 1 }`

### 7. `recommendations`
- Smart optimization recipes.
- **Fields**: `userId`, `fileId`, `fileName`, `fileType`, `fileSize`, `storageSource`, `junkScore`, `detectionReasons`, `recommendedAction`, `riskLevel`, `analysisDate`, `status` (`ACTIVE`, `ACCEPTED`, `REJECTED`, `EXCLUDED`, `PROTECTED`).
- **Indexes**:
  - Compound: `{ userId: 1, status: 1, riskLevel: 1 }`

### 8. `cleanup_history`
- Audited record of disk alterations.
- **Fields**: `userId`, `filesCleanedCount`, `failedCount`, `bytesRecovered`, `storageSource`, `cleanedFileSnapshots`, `isRestored`, `restorationSupported`, `createdAt`.
- **Indexes**:
  - Compound: `{ userId: 1, createdAt: -1 }`

### 9. `notifications`
- In-app notification queue.
- **Indexes**:
  - Compound: `{ userId: 1, isRead: 1, createdAt: -1 }`

### 10. `audit_logs`
- Security event tracking for compliance.
- **Indexes**:
  - Compound: `{ userId: 1, eventType: 1, timestamp: -1 }`

---

## 4. API & Security Architecture

- **Stateless Authentication**: All requests require an `Authorization: Bearer <JWT>` header, verified against HMAC SHA-256 signature and expiration.
- **User Tenant Isolation**: Every controller resolves `userId` from the verified token principal. Repositories enforce ownership filtering at query time.
- **Zero Double-Counting Storage Engine**: `DashboardService` calculates recoverable bytes using distinct file identifier sets, guaranteeing that files matching multiple signals (e.g. duplicate + inactive) are only counted once.
- **Rate Limiting**: Critical endpoints (`/api/scans`, `/api/cleanup`) are shielded by an in-memory token-bucket rate limiter.
- **Encryption at Rest**: Google Drive OAuth refresh tokens are encrypted using AES-256 GCM with unique 12-byte initialization vectors (`IV`) before persistence.
