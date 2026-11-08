/**
 * CloudClean AI - Realistic Sample & Mock Data
 * This data powers the frontend foundation before live backend integration.
 * It is clearly flagged as sample data in the UI.
 */

export const IS_SAMPLE_DATA = true;

export const mockStorageStats = {
  totalCapacityBytes: 1099511627776, // 1 TB
  totalUsedBytes: 904696000000,      // ~842.5 GB
  recoverableBytes: 255977000000,    // ~238.4 GB (28.3% recoverable)
  totalFilesScanned: 42850,
  duplicateFilesCount: 1482,
  duplicateBytes: 95780000000,       // ~89.2 GB
  blurryImagesCount: 348,
  blurryImagesBytes: 15570000000,    // ~14.5 GB
  largeFilesCount: 185,
  largeFilesBytes: 121120000000,     // ~112.8 GB
  inactiveFilesCount: 620,
  inactiveFilesBytes: 52180000000,   // ~48.6 GB
  lastScanTimestamp: '2026-11-08T10:15:00Z',
  healthScore: 68, // Out of 100
};

export const mockStorageConnections = [
  {
    id: 'conn-local-1',
    name: 'Local Computer',
    type: 'local',
    description: 'Connect local folders through a future local-agent daemon/CLI.',
    status: 'connected', // 'connected' | 'disconnected' | 'syncing' | 'error'
    agentVersion: 'v0.9.4-preview',
    folders: [
      { path: 'C:/Users/rocky/Videos/Captures', filesCount: 4200, sizeBytes: 245000000000 },
      { path: 'C:/Users/rocky/Downloads', filesCount: 12400, sizeBytes: 180000000000 },
      { path: 'D:/Projects/Archive', filesCount: 8900, sizeBytes: 95000000000 }
    ],
    totalUsedBytes: 520000000000,
    totalCapacityBytes: 1000000000000,
    lastScanTime: '2026-11-08T09:42:00Z',
    details: 'Monitored via Local Daemon IPC socket. 3 folder trees indexed.',
  },
  {
    id: 'conn-gdrive-1',
    name: 'Google Drive',
    type: 'google_drive',
    description: 'Enterprise cloud drive optimization with OAuth 2.0 access.',
    status: 'disconnected', // 'connected' | 'disconnected' | 'needs_reauth'
    accountEmail: null,
    accountName: null,
    oauthScopes: ['drive.metadata.readonly', 'drive.readonly'],
    totalUsedBytes: 0,
    totalCapacityBytes: 1000000000000, // 1 TB
    lastScanTime: null,
    details: 'OAuth integration ready. Requires backend OAuth server token exchange.',
  }
];

export const mockStorageUsageHistory = [
  { month: 'Jun', usedGB: 580, recoverableGB: 110, cleanedGB: 15 },
  { month: 'Jul', usedGB: 630, recoverableGB: 135, cleanedGB: 28 },
  { month: 'Aug', usedGB: 710, recoverableGB: 168, cleanedGB: 40 },
  { month: 'Sep', usedGB: 775, recoverableGB: 195, cleanedGB: 35 },
  { month: 'Oct', usedGB: 810, recoverableGB: 215, cleanedGB: 52 },
  { month: 'Nov', usedGB: 842, recoverableGB: 238, cleanedGB: 64 },
];

export const mockCategoryDistribution = [
  { name: 'Videos & 4K', value: 345, color: '#3B82F6', percentage: 41 },
  { name: 'Raw & Photos', value: 185, color: '#8B5CF6', percentage: 22 },
  { name: 'Archives & ISOs', value: 140, color: '#06B6D4', percentage: 17 },
  { name: 'Documents & PDFs', value: 95, color: '#10B981', percentage: 11 },
  { name: 'Virtual Disks', value: 50, color: '#F59E0B', percentage: 6 },
  { name: 'Others', value: 27, color: '#64748B', percentage: 3 },
];

export const mockRecentScans = [
  {
    id: 'scan-109',
    target: 'Local Computer (Downloads & Videos)',
    storageType: 'local',
    startTime: '2026-11-08T09:30:12Z',
    completedTime: '2026-11-08T09:42:00Z',
    duration: '11m 48s',
    filesScanned: 25500,
    status: 'completed',
    potentialSavingsBytes: 142000000000,
    flaggedFilesCount: 840,
  },
  {
    id: 'scan-108',
    target: 'Local Computer (Projects/Archive)',
    storageType: 'local',
    startTime: '2026-11-07T14:10:00Z',
    completedTime: '2026-11-07T14:18:22Z',
    duration: '8m 22s',
    filesScanned: 8900,
    status: 'completed',
    potentialSavingsBytes: 45000000000,
    flaggedFilesCount: 312,
  },
  {
    id: 'scan-107',
    target: 'Google Drive (Workspace Backup)',
    storageType: 'google_drive',
    startTime: '2026-11-05T18:00:00Z',
    completedTime: '2026-11-05T18:01:10Z',
    duration: '1m 10s',
    filesScanned: 0,
    status: 'cancelled',
    potentialSavingsBytes: 0,
    flaggedFilesCount: 0,
  },
];

export const mockCleanupRecommendations = [
  {
    id: 'rec-01',
    title: 'Purge 1,482 Exact Duplicate Files',
    category: 'Duplicates',
    type: 'duplicate',
    priority: 'high',
    safetyLevel: 'Safe to Delete', // 'Safe to Delete' | 'Review Recommended' | 'Requires Caution'
    safetyScore: 98,
    potentialSavingsBytes: 95780000000,
    filesCount: 1482,
    description: 'Identified byte-for-byte MD5 and SHA-256 matches across multiple download folders. Preserves the canonical original copy.',
    suggestedAction: 'Keep original copy and delete 1,482 redundant replicas',
    storageLocation: 'Local: C:/Users/rocky/Downloads & Captures',
  },
  {
    id: 'rec-02',
    title: 'Archive 185 Giant Media & ISO Files (>500MB)',
    category: 'Large Files',
    type: 'large',
    priority: 'high',
    safetyLevel: 'Review Recommended',
    safetyScore: 84,
    potentialSavingsBytes: 121120000000,
    filesCount: 185,
    description: 'Stale disk images, high-bitrate OBS screen recordings and video renders unaccessed for over 90 days.',
    suggestedAction: 'Compress or move to cold storage tier',
    storageLocation: 'Local: D:/Projects/Archive',
  },
  {
    id: 'rec-03',
    title: 'Review 348 Blurry & Out-of-Focus Photos',
    category: 'Blurry Images',
    type: 'blurry',
    priority: 'medium',
    safetyLevel: 'Review Recommended',
    safetyScore: 78,
    potentialSavingsBytes: 15570000000,
    filesCount: 348,
    description: 'Laplacian variance blur detection scored these images below the 0.85 clarity threshold.',
    suggestedAction: 'Inspect thumbnail side-by-side and batch discard',
    storageLocation: 'Local: C:/Users/rocky/Pictures/Imports',
  },
  {
    id: 'rec-04',
    title: 'Clean Stale Node Modules & Build Artifacts',
    category: 'Inactive Files',
    type: 'inactive',
    priority: 'low',
    safetyLevel: 'Safe to Delete',
    safetyScore: 99,
    potentialSavingsBytes: 23500000000,
    filesCount: 320,
    description: 'Untouched .cache and build directories from completed client sprints inactive for 180+ days.',
    suggestedAction: 'One-click automated wipe',
    storageLocation: 'Local: C:/Users/rocky/Projects',
  },
];

export const mockFiles = [
  {
    id: 'file-101',
    name: 'docker_desktop_installer_v4.28.exe',
    extension: 'exe',
    fileType: 'application',
    category: 'duplicate',
    sizeBytes: 684200000, // ~652 MB
    storageLocation: 'Local: C:/Users/rocky/Downloads',
    storageType: 'local',
    lastModified: '2026-03-12T14:22:00Z',
    lastAccessed: '2026-03-12T14:22:00Z',
    detectionReason: 'Exact SHA-256 match with file in D:/Installers',
    recommendationStatus: 'Safe to Delete',
    previewUrl: null,
    hash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    metadata: { version: '4.28.0', duplicateCount: 2, duplicateOf: 'docker_desktop_installer_v4.28 (1).exe' },
  },
  {
    id: 'file-102',
    name: 'docker_desktop_installer_v4.28 (1).exe',
    extension: 'exe',
    fileType: 'application',
    category: 'duplicate',
    sizeBytes: 684200000,
    storageLocation: 'Local: D:/Installers',
    storageType: 'local',
    lastModified: '2026-03-12T14:22:00Z',
    lastAccessed: '2026-03-12T14:22:00Z',
    detectionReason: 'Canonical original copy',
    recommendationStatus: 'Keep Original',
    previewUrl: null,
    hash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    metadata: { version: '4.28.0', duplicateCount: 2 },
  },
  {
    id: 'file-103',
    name: 'DSC_0492_raw_burst_shot.jpg',
    extension: 'jpg',
    fileType: 'image',
    category: 'blurry',
    sizeBytes: 18450000, // ~17.6 MB
    storageLocation: 'Local: C:/Users/rocky/Pictures/Imports',
    storageType: 'local',
    lastModified: '2026-08-19T17:45:00Z',
    lastAccessed: '2026-08-20T11:00:00Z',
    detectionReason: 'Blur score 0.38 (Threshold: 0.85). High motion blur detected.',
    recommendationStatus: 'Review & Delete',
    previewUrl: 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=600&auto=format&fit=crop&q=40&blur=8',
    blurScore: 0.38,
    metadata: { resolution: '6000x4000', camera: 'Nikon Z6', iso: 3200, shutter: '1/30s' },
  },
  {
    id: 'file-104',
    name: 'client_presentation_recording_4k.mp4',
    extension: 'mp4',
    fileType: 'video',
    category: 'large',
    sizeBytes: 14800000000, // ~13.78 GB
    storageLocation: 'Local: C:/Users/rocky/Videos/Captures',
    storageType: 'local',
    lastModified: '2026-05-14T10:15:00Z',
    lastAccessed: '2026-05-15T09:00:00Z',
    detectionReason: 'File size exceeds 10 GB limit; unaccessed in 170+ days',
    recommendationStatus: 'Recommended for Archival',
    previewUrl: null,
    metadata: { duration: '01:42:15', codec: 'H.264 / AAC', bitrate: '18 Mbps', resolution: '3840x2160' },
  },
  {
    id: 'file-105',
    name: 'ubuntu-24.04-live-server-amd64.iso',
    extension: 'iso',
    fileType: 'archive',
    category: 'large',
    sizeBytes: 2800000000, // ~2.6 GB
    storageLocation: 'Local: C:/Users/rocky/Downloads',
    storageType: 'local',
    lastModified: '2026-04-26T16:30:00Z',
    lastAccessed: '2026-04-26T17:00:00Z',
    detectionReason: 'Disk image unaccessed for >190 days',
    recommendationStatus: 'Safe to Delete',
    previewUrl: null,
    metadata: { sha256: '8762a11b65...' },
  },
  {
    id: 'file-106',
    name: 'receipt_scan_blurry_008.png',
    extension: 'png',
    fileType: 'image',
    category: 'blurry',
    sizeBytes: 8900000, // ~8.5 MB
    storageLocation: 'Local: C:/Users/rocky/Documents/Receipts',
    storageType: 'local',
    lastModified: '2026-07-02T11:20:00Z',
    lastAccessed: '2026-07-02T11:25:00Z',
    detectionReason: 'Blur score 0.42 (Laplacian low-frequency variance). Text illegible.',
    recommendationStatus: 'Review & Delete',
    previewUrl: 'https://images.unsplash.com/photo-1586075010923-2dd4570fb338?w=600&auto=format&fit=crop&q=40&blur=6',
    blurScore: 0.42,
    metadata: { resolution: '2400x3200', dpi: 300 },
  },
  {
    id: 'file-107',
    name: 'annual_financial_report_2023_v3.pdf',
    extension: 'pdf',
    fileType: 'document',
    category: 'duplicate',
    sizeBytes: 45000000, // ~42.9 MB
    storageLocation: 'Local: C:/Users/rocky/Downloads',
    storageType: 'local',
    lastModified: '2026-01-15T09:40:00Z',
    lastAccessed: '2026-01-16T10:00:00Z',
    detectionReason: 'Exact duplicate of file in Documents/Finances/2023',
    recommendationStatus: 'Safe to Delete',
    previewUrl: null,
    metadata: { pages: 124, author: 'Finance Dept' },
  },
  {
    id: 'file-108',
    name: 'legacy_project_backup_2024.tar.gz',
    extension: 'tar.gz',
    fileType: 'archive',
    category: 'inactive',
    sizeBytes: 8400000000, // ~7.82 GB
    storageLocation: 'Local: D:/Projects/Archive',
    storageType: 'local',
    lastModified: '2025-11-10T12:00:00Z',
    lastAccessed: '2025-11-12T14:00:00Z',
    detectionReason: 'Unaccessed for 360+ days; zero modification activity',
    recommendationStatus: 'Recommended for Archival',
    previewUrl: null,
    metadata: { compressedSize: '7.82 GB', filesContained: 48920 },
  },
  {
    id: 'file-109',
    name: 'raw_wedding_shoot_photo_311.jpg',
    extension: 'jpg',
    fileType: 'image',
    category: 'blurry',
    sizeBytes: 24500000,
    storageLocation: 'Local: C:/Users/rocky/Pictures/Imports',
    storageType: 'local',
    lastModified: '2026-09-04T16:10:00Z',
    lastAccessed: '2026-09-05T08:00:00Z',
    detectionReason: 'Blur score 0.45. Severe lens defocusing detected.',
    recommendationStatus: 'Review & Delete',
    previewUrl: 'https://images.unsplash.com/photo-1519741497674-611481863552?w=600&auto=format&fit=crop&q=40&blur=8',
    blurScore: 0.45,
    metadata: { resolution: '7952x5304', camera: 'Sony A7R IV' },
  },
  {
    id: 'file-110',
    name: 'machine_learning_model_weights_epoch_50.ckpt',
    extension: 'ckpt',
    fileType: 'data',
    category: 'large',
    sizeBytes: 12400000000, // ~11.5 GB
    storageLocation: 'Local: D:/Projects/ML_Runs',
    storageType: 'local',
    lastModified: '2026-06-18T20:30:00Z',
    lastAccessed: '2026-06-19T02:00:00Z',
    detectionReason: 'Intermediate checkpoint superseded by epoch 100 weights',
    recommendationStatus: 'Safe to Delete',
    previewUrl: null,
    metadata: { framework: 'PyTorch Lightning', parameters: '1.2B' },
  },
  {
    id: 'file-111',
    name: 'marketing_pitch_deck_final_v2_edit.pptx',
    extension: 'pptx',
    fileType: 'document',
    category: 'duplicate',
    sizeBytes: 85000000, // ~81 MB
    storageLocation: 'Local: C:/Users/rocky/Downloads',
    storageType: 'local',
    lastModified: '2026-07-22T15:10:00Z',
    lastAccessed: '2026-07-22T15:15:00Z',
    detectionReason: 'Byte-for-byte duplicate found in Cloud Documents',
    recommendationStatus: 'Safe to Delete',
    previewUrl: null,
    metadata: { slides: 45, containsVideo: true },
  },
  {
    id: 'file-112',
    name: 'old_cache_temp_dump_build.log',
    extension: 'log',
    fileType: 'text',
    category: 'inactive',
    sizeBytes: 1950000000, // ~1.81 GB
    storageLocation: 'Local: C:/Users/rocky/AppData/Local/Temp',
    storageType: 'local',
    lastModified: '2026-02-14T08:00:00Z',
    lastAccessed: '2026-02-14T08:00:00Z',
    detectionReason: 'Debug crash log inactive for 260+ days',
    recommendationStatus: 'Safe to Delete',
    previewUrl: null,
    metadata: { linesCount: 14205000 },
  },
];

export const mockCleanupHistory = [
  {
    id: 'clean-301',
    timestamp: '2026-11-06T15:45:00Z',
    action: 'Purged 420 Duplicate Downloads',
    targetCategory: 'Duplicates',
    filesAffected: 420,
    freedSpaceBytes: 34500000000, // 34.5 GB
    status: 'success',
    executedBy: 'Rocky (User Confirmation)',
    method: 'Safe Trash / Soft Delete',
  },
  {
    id: 'clean-300',
    timestamp: '2026-11-01T11:20:00Z',
    action: 'Discarded 112 Blurry Burst Photos',
    targetCategory: 'Blurry Images',
    filesAffected: 112,
    freedSpaceBytes: 5200000000, // 5.2 GB
    status: 'success',
    executedBy: 'Rocky (Batch Review)',
    method: 'Local Recycle Bin',
  },
  {
    id: 'clean-299',
    timestamp: '2026-10-24T09:15:00Z',
    action: 'Archived 18 Stale 4K OBS Recordings',
    targetCategory: 'Large Files',
    filesAffected: 18,
    freedSpaceBytes: 86400000000, // 86.4 GB
    status: 'success',
    executedBy: 'Automated Retention Policy',
    method: 'Compressed & Moved to Archive',
  },
  {
    id: 'clean-298',
    timestamp: '2026-10-15T18:30:00Z',
    action: 'Wiped Inactive Temp Caches',
    targetCategory: 'Inactive Files',
    filesAffected: 1450,
    freedSpaceBytes: 14200000000, // 14.2 GB
    status: 'success',
    executedBy: 'Rocky (Quick Clean)',
    method: 'Permanent Shred',
  },
];

export const mockUserSettings = {
  profile: {
    fullName: 'Rocky Balboa',
    email: 'rocky@example.com',
    role: 'Storage Administrator',
    company: 'CloudClean Enterprise Labs',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
  },
  scannerConfig: {
    autoScanFrequency: 'weekly', // 'daily' | 'weekly' | 'manual'
    minLargeFileSizeMB: 500,
    blurThresholdPercent: 85,
    inactiveDaysThreshold: 180,
    excludeHiddenFolders: true,
    excludeExtensions: ['.sys', '.dll', '.tmp', '.git'],
  },
  cloudConfig: {
    googleDriveAutoSync: false,
    googleDriveSyncIntervalHours: 12,
    enableLocalAgentAutoStart: true,
    localAgentPort: 9876,
  },
  notifications: {
    emailWeeklyReport: true,
    alertOnStorageThreshold: true,
    storageThresholdPercent: 80,
    notifyOnScanComplete: true,
  }
};

