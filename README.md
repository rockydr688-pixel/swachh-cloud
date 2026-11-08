# CloudClean AI – Intelligent Cloud Storage Optimization Platform
> **Frontend Foundation & Client Architecture**

CloudClean AI is an enterprise-grade SaaS dashboard designed to analyze, detect, and optimize storage waste across local filesystems and cloud storage repositories.

---

## 🚀 Technology Stack

- **Framework**: React.js 18 with Vite
- **Styling**: Tailwind CSS with custom design system tokens
- **Routing**: React Router v7 (`react-router-dom`)
- **Icons**: Lucide React
- **Analytics & Charts**: Recharts
- **HTTP Client**: Axios with configured request/response interceptors
- **Font**: Inter (Google Fonts)

---

## 🎨 Design System

- **Sidebar**: Dark navy (`#0B132B` / `bg-navy-900`) vertical sidebar with crisp section grouping and glowing status indicators.
- **Main Canvas**: Light background (`#F8FAFC` / `bg-slate-50`) with subtle border lines (`border-slate-200/80`).
- **Accent Palette**: 
  - Indigo / Blue (`#4F46E5`, `#2563EB`)
  - Violet (`#7C3AED`, `#8B5CF6`)
  - Emerald (`#10B981`) for safe deletions and healthy stats
  - Amber / Rose for warnings and irreversible actions
- **Typography**: Clean, high-legibility Inter typography with tabular numbers.
- **Feedback States**: Full coverage of Loading spinners, Empty states, Error alerts, and Confirmation modals.

---

## 📁 13 Built Pages

1. **Dashboard** (`/dashboard`): Real-time capacity gauge, 4 core KPI stat cards, historical usage area chart, category donut breakdown, recent recommendations, and scan log feed.
2. **Storage Connections** (`/connections`): Two dedicated connection cards:
   - **Local Computer**: Daemon IPC integration status, monitored directories, last scan time.
   - **Google Drive**: OAuth status card strictly adhering to the architectural standard (non-simulated; indicates real OAuth backend server requirement).
3. **File Scanner** (`/scanner`): Multi-threaded heuristic scanner interface with live progress simulation, console debug stream, and historical scan audit table.
4. **All Files** (`/files`): Complete file management interface with real-time search, category tabs, sorting by name/size/date, pagination, and file preview inspection modal.
5. **Duplicate Files** (`/files/duplicates`): Identifies exact SHA-256 and MD5 byte clones, provides a 1-click "Smart Clean" button preserving canonical original files.
6. **Blurry Images** (`/files/blurry`): Computer vision Laplacian variance analyzer flagging unfocused/low-clarity photos below 85% clarity with side-by-side gallery/table toggle.
7. **Large Files** (`/files/large`): Surfaces files >500 MB (ISOs, virtual machine images, 4K screen recordings) with size brackets and archival actions.
8. **Inactive Files** (`/files/inactive`): Stale file detector auditing assets unaccessed for >180 days.
9. **Cleanup Recommendations** (`/recommendations`): Prioritized optimization recipes with safety scores (98% safe), estimated recovery GB, and execution confirmation modals.
10. **Cleanup History** (`/history`): Cryptographically audited log of past purge and archive actions with CSV export.
11. **Reports and Analytics** (`/reports`): Capacity runway forecast, monthly accumulation velocity, and cloud tier ROI cost savings.
12. **User Settings** (`/settings`): Profile management, heuristic sensitivity thresholds (blur clarity %, large file cutoff MB, inactive days cutoff), and notification preferences.
13. **Login and Registration** (`/auth`): Modern SaaS authentication interface with quick 1-click demo login.

---

## 🛠️ Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Run Development Server
```bash
npm run dev
```
The server will start at `http://localhost:3000`.

### 3. Build for Production
```bash
npm run build
```
Creates an optimized, chunked production bundle in `/dist`.

---

## 🔒 Security & Backend Integration Rules

- **Zero Hardcoded Credentials**: Authentication tokens and mock credentials are stored in localStorage with clean session guards.
- **Separation of Concerns**: UI components interact only with service abstraction layers (`src/services/api.js`, `src/services/fileService.js`, `src/services/storageService.js`, `src/services/scanService.js`).
- **Configurable Endpoints**: Base API endpoint defaults to `VITE_API_BASE_URL` in `.env`.
- **Google Drive OAuth**: Real authorization requires backend token exchange endpoints in Part 2.



# swachh-cloud
