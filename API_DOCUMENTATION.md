# CloudClean AI – REST API Documentation

CloudClean AI provides a standardized RESTful API surface. All requests, except authentication endpoints, require an HTTP Bearer JWT token in the `Authorization` header:

```http
Authorization: Bearer <your_jwt_token>
```

All responses conform to the standard `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Operation completed successfully.",
  "data": { ... },
  "timestamp": "2026-11-18T10:00:00Z",
  "status": 200
}
```

---

## 1. Authentication Endpoints (`/api/auth`)

### Register User
- **Method / Route**: `POST /api/auth/register`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "fullName": "Jane Doe",
    "email": "jane@cloudclean.ai",
    "password": "SecurePassword123!"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "User registered successfully",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
      "user": {
        "id": "usr-101",
        "email": "jane@cloudclean.ai",
        "fullName": "Jane Doe",
        "role": "ROLE_USER"
      }
    }
  }
  ```

### User Login
- **Method / Route**: `POST /api/auth/login`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "email": "jane@cloudclean.ai",
    "password": "SecurePassword123!"
  }
  ```
- **Response**: `200 OK` (Returns JWT token and user profile)

### Current User Profile
- **Method / Route**: `GET /api/auth/me`
- **Auth**: Bearer JWT
- **Response**: `200 OK` with user details.

---

## 2. Storage Connections (`/api/storage`)

### List Connections
- **Method / Route**: `GET /api/storage`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Array of connected drives, quotas, and sync states)

### Provider Status Catalog
- **Method / Route**: `GET /api/storage/providers/status`
- **Auth**: Bearer JWT
- **Description**: Returns configuration readiness, live connection counts, setup guides, and generated OAuth authorization URLs for Google Drive, OneDrive, Dropbox, and Local Storage.
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Storage provider status retrieved successfully",
    "data": [
      {
        "provider": "google_drive",
        "displayName": "Google Drive",
        "configured": true,
        "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?...",
        "connectedAccountsCount": 1,
        "setupInstructions": "Configured and active."
      },
      {
        "provider": "onedrive",
        "displayName": "Microsoft OneDrive",
        "configured": false,
        "authUrl": null,
        "connectedAccountsCount": 0,
        "setupInstructions": "Missing ONEDRIVE_CLIENT_ID or ONEDRIVE_CLIENT_SECRET in .env."
      }
    ]
  }
  ```

### Universal Cloud Account Connection
- **Method / Route**: `POST /api/storage/cloud/connect`
- **Auth**: Bearer JWT
- **Description**: Universal OAuth 2.0 authorization code exchange supporting Google Drive, Microsoft OneDrive, and Dropbox. Tokens are encrypted using AES-256 GCM before database storage.
- **Request Body**:
  ```json
  {
    "provider": "onedrive",
    "authCode": "M.R3_BAY.0a1b2c3d...",
    "redirectUri": "http://localhost:3000/oauth/callback",
    "accountName": "Work OneDrive"
  }
  ```
- **Response**: `200 OK` (Returns the newly created StorageConnection record)

### Register Local Folders
- **Method / Route**: `POST /api/storage/local/register`
- **Auth**: Bearer JWT
- **Request Body**:
  ```json
  {
    "connectionName": "Local Workstation",
    "monitoredPaths": [
      "F:\\Downloads",
      "F:\\ProjectArchives"
    ]
  }
  ```

### Connect Google Drive (Legacy)
- **Method / Route**: `POST /api/storage/google/connect`
- **Auth**: Bearer JWT
- **Request Body**:
  ```json
  {
    "authCode": "4/0AeanS0...",
    "redirectUri": "http://localhost:3000/oauth/callback"
  }
  ```

---

## 3. Storage Scanning (`/api/scans`)

### Launch Storage Scan
- **Method / Route**: `POST /api/scans`
- **Auth**: Bearer JWT
- **Request Body**:
  ```json
  {
    "target": "Local Computer",
    "storageType": "local",
    "options": {
      "detectDuplicates": true,
      "analyzeImages": true,
      "checkInactivity": true
    }
  }
  ```
- **Response**: `202 Accepted`
  ```json
  {
    "success": true,
    "message": "Scan job successfully scheduled.",
    "data": {
      "id": "scan-202",
      "targetStorage": "Local Computer",
      "status": "IN_PROGRESS",
      "startedAt": "2026-11-18T10:15:00Z"
    }
  }
  ```

### Get Scan Status
- **Method / Route**: `GET /api/scans/{id}`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (includes `status`, `progress`, `filesScanned`, `bytesScanned`)

### Live Scan Progress Stream (Server-Sent Events)
- **Method / Route**: `GET /api/scans/{id}/progress/stream`
- **Auth**: Bearer JWT
- **Accept**: `text/event-stream`
- **Description**: Subscribes to real-time Server-Sent Events (SSE) emitting progress snapshots across all scanning passes.
- **Event Data Structure**:
  ```json
  {
    "scanId": "scan-202",
    "phase": "HASHING_DUPLICATES",
    "message": "Calculated hashes for 150/320 duplicate candidate files...",
    "filesDiscovered": 1540,
    "filesProcessed": 150,
    "bytesProcessed": 450000000,
    "percentComplete": 45,
    "status": "IN_PROGRESS",
    "timestamp": 1731928500000
  }
  ```
- **Lifecycle Phases**:
  - `DISCOVERING_FILES`: Metadata crawl across local folders or cloud API tree pagination.
  - `HASHING_DUPLICATES`: Targeted streaming SHA-256 computation for potential duplicates.
  - `AI_IMAGE_ANALYSIS`: OpenCV blur, low-light, and resolution audit on photos.
  - `LARGE_FILE_AUDIT`: Heavy file categorization and footprint analysis.
  - `INACTIVE_FILE_AUDIT`: File staleness checks against retention thresholds.
  - `JUNK_SCORING`: Multi-factor heuristics score computation.
  - `COMPLETED`: Persisted audit results in MongoDB.

### Cancel Scan
- **Method / Route**: `POST /api/scans/{id}/cancel`
- **Auth**: Bearer JWT
- **Response**: `200 OK`

---

## 4. File Management & Detection (`/api/files`, `/api/detection`)

### List / Search Files
- **Method / Route**: `GET /api/files`
- **Auth**: Bearer JWT
- **Query Parameters**:
  - `search`: Filter by name or path
  - `category`: `all`, `duplicate`, `blurry`, `large`, `inactive`
  - `sortBy`: `size`, `name`, `date`
  - `sortOrder`: `asc`, `desc`
  - `page`: Page number (default: 1)
  - `limit`: Items per page (default: 20)
- **Response**: `200 OK` (Paginated file list with total counters)

### Get Duplicates Report
- **Method / Route**: `GET /api/detection/duplicates`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Duplicate clusters grouped by SHA-256 hash)

### Get Blurry Images Report
- **Method / Route**: `GET /api/detection/blurry?threshold=100.0`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Images flagged with low Laplacian variance)

---

## 5. Smart Recommendations (`/api/recommendations`)

### List Recommendations
- **Method / Route**: `GET /api/recommendations`
- **Auth**: Bearer JWT
- **Query Parameters**: `category`, `riskLevel` (`low`, `medium`, `high`), `status` (`ACTIVE`, `ACCEPTED`, `REJECTED`)
- **Response**: `200 OK` (List of recipes with junk score and reasons)

### Accept Recommendation
- **Method / Route**: `POST /api/recommendations/{id}/accept`
- **Auth**: Bearer JWT

### Reject Recommendation
- **Method / Route**: `POST /api/recommendations/{id}/reject`
- **Auth**: Bearer JWT

### Whitelist / Protect File
- **Method / Route**: `POST /api/recommendations/files/{fileId}/protect`
- **Auth**: Bearer JWT
- **Description**: Sets `isProtected = true`. Permanently shields file from purges.

### Unprotect File
- **Method / Route**: `POST /api/recommendations/files/{fileId}/unprotect`
- **Auth**: Bearer JWT

---

## 6. Safe Cleanup Center (`/api/cleanup`)

### Calculate Cleanup Impact
- **Method / Route**: `POST /api/cleanup/impact`
- **Auth**: Bearer JWT
- **Request Body**:
  ```json
  {
    "fileIds": ["file-101", "file-102"]
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "data": {
      "totalCandidateFiles": 2,
      "totalCandidateBytes": 4508500000,
      "formattedCandidateSize": "4.2 GB",
      "protectedFilesCount": 0,
      "netRecoverableFiles": 2,
      "netRecoverableBytes": 4508500000,
      "formattedNetRecoverableSize": "4.2 GB",
      "warningMessage": "2 item(s) selected for cleanup."
    }
  }
  ```

### Execute Cleanup (Enforced Confirmation)
- **Method / Route**: `POST /api/cleanup`
- **Auth**: Bearer JWT
- **Request Body**:
  ```json
  {
    "fileIds": ["file-101", "file-102"],
    "confirmed": true
  }
  ```
- **Note**: Passing `confirmed: false` or omitting `confirmed` results in `400 Bad Request`.
- **Response**: `200 OK`

### Restore Files from Trash
- **Method / Route**: `POST /api/cleanup/{id}/restore`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Reverts cleaned files back to active filesystem)

### Cleanup History
- **Method / Route**: `GET /api/cleanup/history`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Audit log of all cleanup operations)

---

## 7. Reports & Analytics (`/api/reports`, `/api/dashboard`)

### Dashboard Metrics (Zero Double-Counted)
- **Method / Route**: `GET /api/dashboard`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Total storage, recoverable storage, file counters, health score)

### Reports Overview
- **Method / Route**: `GET /api/reports/overview`
- **Auth**: Bearer JWT

### CSV Export
- **Method / Route**: `GET /api/reports/export/csv?type=files|cleanup|scans`
- **Auth**: Bearer JWT
- **Response**: `text/csv` stream with RFC-4180 formatting.

---

## 8. Notifications (`/api/notifications`)

- `GET /api/notifications` – Fetch unread and historical notifications
- `PATCH /api/notifications/{id}/read` – Mark single notification read
- `POST /api/notifications/read-all` – Mark all notifications read
- `DELETE /api/notifications/{id}` – Dismiss notification

---

## 9. Local Devices Management (`/api/devices`)

### List Registered Devices
- **Method / Route**: `GET /api/devices`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Array of registered local client agent machines, approved folders, and heartbeat status)

### Register / Sync Local Device
- **Method / Route**: `POST /api/devices`
- **Auth**: Bearer JWT
- **Request Body**:
  ```json
  {
    "deviceName": "MacBook Pro M3",
    "osName": "macOS 14.5",
    "hostname": "mbp-m3.local",
    "agentVersion": "1.0.0",
    "approvedFolders": ["/Users/rocky/Downloads", "/Users/rocky/Documents"]
  }
  ```
- **Response**: `200 OK` (Returns registered/updated device record)

### Device Heartbeat
- **Method / Route**: `POST /api/devices/{id}/heartbeat`
- **Auth**: Bearer JWT
- **Response**: `200 OK` (Updates `lastHeartbeat` and transitions status to `ONLINE`)

### Unregister Device
- **Method / Route**: `DELETE /api/devices/{id}`
- **Auth**: Bearer JWT
- **Response**: `200 OK`

---

## 10. User Preferences (`/api/users/preferences`)

### Get Current Preferences
- **Method / Route**: `GET /api/users/preferences`
- **Auth**: Bearer JWT
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "data": {
      "autoScanFrequency": "WEEKLY",
      "inactivityDaysThreshold": 90,
      "largeFileThresholdMb": 100,
      "blurVarianceThreshold": 100.0,
      "protectedExtensions": [".pem", ".key", ".env", ".keystore"],
      "emailAlertsEnabled": true,
      "inAppAlertsEnabled": true,
      "theme": "DARK"
    }
  }
  ```

### Update Preferences
- **Method / Route**: `PUT /api/users/preferences`
- **Auth**: Bearer JWT
- **Request Body**:
  ```json
  {
    "autoScanFrequency": "DAILY",
    "inactivityDaysThreshold": 60,
    "largeFileThresholdMb": 250,
    "blurVarianceThreshold": 120.0,
    "emailAlertsEnabled": false,
    "theme": "DARK"
  }
  ```
- **Response**: `200 OK` (Persists new threshold values to MongoDB)

---

## 11. Error Codes & Handling

| HTTP Code | Error Condition | Response Format |
| :--- | :--- | :--- |
| **`400 Bad Request`** | Validation failure or missing explicit cleanup confirmation | `{ "success": false, "message": "Explicit user confirmation is strictly required..." }` |
| **`401 Unauthorized`** | Missing, invalid, or expired Bearer JWT | `{ "success": false, "message": "Unauthorized" }` |
| **`403 Forbidden`** | Cross-tenant access attempt (resource owned by another user) | `{ "success": false, "message": "Access denied" }` |
| **`404 Not Found`** | Resource (file, scan, connection) not found | `{ "success": false, "message": "Resource not found" }` |
| **`429 Too Many Requests`** | Rate limit exceeded on sensitive scan or cleanup endpoint | `{ "success": false, "message": "Rate limit exceeded. Please wait..." }` |
| **`500 Internal Error`** | Uncaught application exception | `{ "success": false, "message": "An internal error occurred." }` |
