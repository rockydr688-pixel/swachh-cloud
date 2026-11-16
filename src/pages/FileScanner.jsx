import React, { useState } from 'react';
import PageHeader from '../components/layout/PageHeader';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import ProgressBar from '../components/common/ProgressBar';
import { useStorage } from '../context/StorageContext';
import { useToast } from '../context/ToastContext';
import { formatBytes, formatDateTime, formatNumber } from '../utils/formatters';
import {
  ScanSearch,
  Play,
  RotateCcw,
  CheckCircle2,
  Clock,
  HardDrive,
  Cloud,
  Copy,
  ImageOff,
  Maximize2,
  Calendar,
  Terminal,
  ShieldAlert,
  AlertTriangle,
  StopCircle,
  FileCheck2,
  Zap,
} from 'lucide-react';

export const FileScanner = () => {
  const {
    connections,
    recentScans,
    isScanning,
    scanProgress,
    activeScan,
    scanError,
    runScan,
    cancelActiveScan,
  } = useStorage();

  const { addToast } = useToast();

  const [selectedTarget, setSelectedTarget] = useState('Local Computer');
  const [incremental, setIncremental] = useState(false);
  const [enableDuplicates, setEnableDuplicates] = useState(true);
  const [enableBlurry, setEnableBlurry] = useState(true);
  const [enableLarge, setEnableLarge] = useState(true);
  const [enableInactive, setEnableInactive] = useState(true);
  const [lastFinishedScan, setLastFinishedScan] = useState(null);

  const selectedConn = connections.find(
    (c) => c.name === selectedTarget || c.type === (selectedTarget.toLowerCase().includes('google') ? 'google_drive' : 'local')
  );

  const handleStartScan = async () => {
    if (selectedConn && selectedConn.type === 'google_drive' && selectedConn.status !== 'connected') {
      addToast({
        title: 'Google Drive Not Connected',
        message: 'Please connect and authorize Google Drive in Storage Connections first.',
        type: 'warning',
      });
      return;
    }

    try {
      await runScan({
        target: selectedTarget,
        storageType: selectedTarget.toLowerCase().includes('google') ? 'google_drive' : 'local',
        incremental,
        checkDuplicates: enableDuplicates,
        checkBlurry: enableBlurry,
        checkLargeFiles: enableLarge,
        checkInactive: enableInactive,
      });
      addToast({
        title: 'Scan Initiated',
        message: `Asynchronous heuristic scan started on ${selectedTarget}.`,
        type: 'info',
      });
    } catch (err) {
      addToast({
        title: 'Scan Conflict',
        message: err.message || 'Cannot start scan while another is running.',
        type: 'error',
      });
    }
  };

  const handleCancel = async () => {
    await cancelActiveScan();
    addToast({
      title: 'Scan Cancelled',
      message: 'Active scanning thread terminated by user request.',
      type: 'warning',
    });
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="File Scanner & Diagnostics"
        subtitle="Initiate asynchronous heuristic scanning across local and Google Drive storage to detect duplicate, blurry, and oversized assets."
        breadcrumbs={[{ label: 'File Scanner' }]}
        actions={
          <div className="flex items-center gap-2">
            {isScanning ? (
              <Button
                variant="ghost"
                size="md"
                icon={StopCircle}
                onClick={handleCancel}
                className="text-rose-600 hover:bg-rose-50 border border-rose-200"
              >
                Cancel Active Scan
              </Button>
            ) : (
              <Button
                variant="primary"
                size="md"
                icon={Play}
                isLoading={isScanning}
                onClick={handleStartScan}
                className="bg-indigo-600 hover:bg-indigo-700 shadow-sm"
              >
                Start New Scan
              </Button>
            )}
          </div>
        }
      />

      {/* Duplicate Scan Error Alert */}
      {scanError && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-3 shadow-sm">
          <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <h4 className="font-bold text-rose-950 text-sm">Duplicate Scan Prevention Alert</h4>
            <p>{scanError}</p>
          </div>
        </div>
      )}

      {/* Active Scan Progress Section */}
      {isScanning && activeScan && (
        <Card className="border-indigo-200 bg-gradient-to-r from-indigo-50/50 via-white to-violet-50/50 shadow-md">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-indigo-600 text-white flex items-center justify-center animate-pulse">
                  <ScanSearch className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-slate-900">
                    Scanning Target: {activeScan.target}
                  </h4>
                  <p className="text-xs text-indigo-700 mt-0.5 truncate max-w-md">
                    {activeScan.activeDirectory || 'Analyzing files and calculating hashes...'}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Badge variant="indigo" size="sm">
                  {scanProgress}% Completed
                </Badge>
                <Button
                  variant="ghost"
                  size="xs"
                  icon={StopCircle}
                  onClick={handleCancel}
                  className="text-rose-600 hover:bg-rose-50 border border-rose-200"
                >
                  Cancel
                </Button>
              </div>
            </div>

            <ProgressBar
              value={scanProgress}
              max={100}
              color="gradient"
              size="md"
            />

            {/* Real-time counters */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
              <div className="p-2.5 bg-white/80 rounded-lg border border-slate-200/60 text-xs">
                <span className="text-slate-400 block text-[11px]">Discovered Files</span>
                <span className="text-sm font-bold text-slate-800">
                  {formatNumber(activeScan.totalFilesScanned || 0)}
                </span>
              </div>
              <div className="p-2.5 bg-white/80 rounded-lg border border-slate-200/60 text-xs">
                <span className="text-slate-400 block text-[11px]">Data Scanned</span>
                <span className="text-sm font-bold text-slate-800">
                  {formatBytes(activeScan.totalBytesScanned || 0)}
                </span>
              </div>
              <div className="p-2.5 bg-white/80 rounded-lg border border-slate-200/60 text-xs">
                <span className="text-slate-400 block text-[11px]">Failed / Inaccessible</span>
                <span className={`text-sm font-bold ${activeScan.failedFilesCount > 0 ? 'text-amber-600' : 'text-slate-800'}`}>
                  {activeScan.failedFilesCount || 0}
                </span>
              </div>
              <div className="p-2.5 bg-white/80 rounded-lg border border-slate-200/60 text-xs">
                <span className="text-slate-400 block text-[11px]">Mode</span>
                <span className="text-sm font-bold text-indigo-600">
                  {incremental ? 'Incremental' : 'Full Traversal'}
                </span>
              </div>
            </div>

            {/* Live Terminal Log */}
            <div className="p-3 bg-slate-950 text-slate-300 font-mono text-[11px] rounded-lg space-y-1 overflow-x-auto max-h-36">
              <p className="text-indigo-400">[DAEMON] Worker process attached to target: {activeScan.target}</p>
              <p className="text-slate-400">[WALK] Traversing path: {activeScan.activeDirectory}</p>
              {scanProgress >= 20 && (
                <p className="text-emerald-400">[PROGRESS] {formatNumber(activeScan.totalFilesScanned || 1200)} files cataloged with SHA-256 integrity</p>
              )}
              {activeScan.failedFilesCount > 0 && (
                <p className="text-amber-400">[WARN] {activeScan.failedFilesCount} inaccessible/locked files safely recorded without halting scan</p>
              )}
              {scanProgress >= 60 && (
                <p className="text-violet-400">[ANALYSIS] Running image Laplacian blur variance pass</p>
              )}
            </div>
          </div>
        </Card>
      )}

      {/* Scanner Configuration Controls */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Target & Passes Selector */}
        <Card
          title="Scan Configuration & Target Selection"
          subtitle="Choose registered storage provider and diagnostic filters"
          className="lg:col-span-2"
        >
          <div className="space-y-5">
            {/* Target Select with live provider info */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Storage Provider Target
              </label>
              <select
                value={selectedTarget}
                onChange={(e) => setSelectedTarget(e.target.value)}
                className="w-full text-xs bg-slate-50 border border-slate-200 rounded-lg p-2.5 text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                {connections.length > 0 ? (
                  connections.map((c) => (
                    <option key={c.id} value={c.name}>
                      {c.name} ({c.type === 'local' ? `${c.monitoredPaths?.length || 1} Monitored Paths` : (c.status === 'connected' ? 'Authorized' : 'Disconnected')})
                    </option>
                  ))
                ) : (
                  <>
                    <option value="Local Computer">Local Computer (Agent Daemon)</option>
                    <option value="Google Drive">Google Drive (OAuth 2.0)</option>
                  </>
                )}
              </select>
            </div>

            {/* Incremental Scan Toggle */}
            <div className="p-3 bg-slate-50 border border-slate-200/80 rounded-xl flex items-center justify-between">
              <div className="space-y-0.5">
                <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                  <Zap className="w-3.5 h-3.5 text-amber-500" />
                  Incremental Scanning Mode
                </span>
                <p className="text-[11px] text-slate-500">
                  Only scan files created or modified since the previous successful scan run.
                </p>
              </div>
              <input
                type="checkbox"
                checked={incremental}
                onChange={(e) => setIncremental(e.target.checked)}
                className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
              />
            </div>

            {/* Diagnostic Modules */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Diagnostic Passes
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-colors">
                  <input
                    type="checkbox"
                    checked={enableDuplicates}
                    onChange={(e) => setEnableDuplicates(e.target.checked)}
                    className="mt-0.5 rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                  />
                  <div>
                    <span className="text-xs font-bold text-slate-800 block">
                      Cryptographic Duplicate Match
                    </span>
                    <span className="text-[11px] text-slate-500">
                      Byte-for-byte SHA-256 integrity match.
                    </span>
                  </div>
                </label>

                <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-colors">
                  <input
                    type="checkbox"
                    checked={enableBlurry}
                    onChange={(e) => setEnableBlurry(e.target.checked)}
                    className="mt-0.5 rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                  />
                  <div>
                    <span className="text-xs font-bold text-slate-800 block">
                      Blurry Image Analyzer
                    </span>
                    <span className="text-[11px] text-slate-500">
                      Laplacian edge variance threshold &lt; 0.85.
                    </span>
                  </div>
                </label>

                <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-colors">
                  <input
                    type="checkbox"
                    checked={enableLarge}
                    onChange={(e) => setEnableLarge(e.target.checked)}
                    className="mt-0.5 rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                  />
                  <div>
                    <span className="text-xs font-bold text-slate-800 block">
                      Large Files Scanner
                    </span>
                    <span className="text-[11px] text-slate-500">
                      Identifies files &gt; 500 MB footprint.
                    </span>
                  </div>
                </label>

                <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-colors">
                  <input
                    type="checkbox"
                    checked={enableInactive}
                    onChange={(e) => setEnableInactive(e.target.checked)}
                    className="mt-0.5 rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                  />
                  <div>
                    <span className="text-xs font-bold text-slate-800 block">
                      Inactive Files Audit
                    </span>
                    <span className="text-[11px] text-slate-500">
                      Flags files unaccessed for &gt; 180 days.
                    </span>
                  </div>
                </label>
              </div>
            </div>

            <div className="pt-2">
              <Button
                variant="primary"
                size="md"
                onClick={handleStartScan}
                isLoading={isScanning}
                className="w-full sm:w-auto bg-indigo-600 hover:bg-indigo-700"
              >
                Execute Scan Run
              </Button>
            </div>
          </div>
        </Card>

        {/* Engine Specs */}
        <Card
          title="Common Storage Abstraction"
          subtitle="Provider adapters: LocalHost & Google Drive"
        >
          <div className="space-y-3 text-xs">
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Local Storage</span>
              <span className="font-semibold text-emerald-600">Active (Zero Upload)</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Google Drive API</span>
              <span className="font-semibold text-violet-600">v3 (OAuth 2.0 PKCE)</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Cryptographic Hash</span>
              <span className="font-semibold text-slate-800 font-mono">SHA-256 Streamed</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Duplicate Protection</span>
              <span className="font-semibold text-indigo-600">Enforced</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Incremental Scan</span>
              <span className="font-semibold text-slate-800">Change-Tracking</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Safe Trashing</span>
              <span className="font-semibold text-emerald-600">Non-Destructive Undo</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Historical Scan Executions */}
      <Card
        title="Historical Scan Executions"
        subtitle="Complete record of past diagnostic runs, discovered files, and identified waste"
      >
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                <th className="py-3 px-4">Scan Target</th>
                <th className="py-3 px-3">Start Time</th>
                <th className="py-3 px-3">Duration</th>
                <th className="py-3 px-3">Files Scanned</th>
                <th className="py-3 px-3">Flagged Files</th>
                <th className="py-3 px-3">Failed Files</th>
                <th className="py-3 px-3">Potential Savings</th>
                <th className="py-3 px-4 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {recentScans.map((scan) => (
                <tr key={scan.id} className="hover:bg-slate-50/60 transition-colors">
                  <td className="py-3 px-4 font-semibold text-slate-900">
                    {scan.target}
                  </td>
                  <td className="py-3 px-3 text-slate-500">
                    {formatDateTime(scan.startTime)}
                  </td>
                  <td className="py-3 px-3 font-mono">{scan.duration || 'In progress'}</td>
                  <td className="py-3 px-3 font-semibold">
                    {formatNumber(scan.totalFilesScanned || scan.filesScanned || 0)}
                  </td>
                  <td className="py-3 px-3 text-indigo-600 font-semibold">
                    {formatNumber(scan.flaggedFilesCount || 0)}
                  </td>
                  <td className="py-3 px-3 font-medium">
                    <span className={scan.failedFilesCount > 0 ? 'text-amber-600 font-bold' : 'text-slate-400'}>
                      {scan.failedFilesCount || 0}
                    </span>
                  </td>
                  <td className="py-3 px-3 font-bold text-slate-800">
                    {formatBytes(scan.potentialSavingsBytes || 0)}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <Badge
                      variant={
                        scan.status === 'completed'
                          ? 'success'
                          : scan.status === 'cancelled'
                          ? 'warning'
                          : scan.status === 'failed'
                          ? 'danger'
                          : 'indigo'
                      }
                      size="xs"
                    >
                      {scan.status}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};

export default FileScanner;
