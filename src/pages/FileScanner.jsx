import React, { useState } from 'react';
import PageHeader from '../components/layout/PageHeader';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import ProgressBar from '../components/common/ProgressBar';
import { useStorage } from '../context/StorageContext';
import { formatBytes, formatDateTime, formatNumber } from '../utils/formatters';
import {
  ScanSearch,
  Play,
  RotateCcw,
  CheckCircle2,
  Clock,
  HardDrive,
  Copy,
  ImageOff,
  Maximize2,
  Calendar,
  Terminal,
  ShieldAlert,
} from 'lucide-react';

export const FileScanner = () => {
  const { recentScans, isScanning, scanProgress, runScan } = useStorage();
  const [selectedTarget, setSelectedTarget] = useState('Local Computer (Downloads & Videos)');
  const [enableDuplicates, setEnableDuplicates] = useState(true);
  const [enableBlurry, setEnableBlurry] = useState(true);
  const [enableLarge, setEnableLarge] = useState(true);
  const [enableInactive, setEnableInactive] = useState(true);

  const handleStartScan = () => {
    runScan(selectedTarget);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="File Scanner & Diagnostics"
        subtitle="Initiate multi-threaded heuristic scanning across filesystems to identify redundant, blurry, and oversized assets."
        breadcrumbs={[{ label: 'File Scanner' }]}
        actions={
          <Button
            variant="primary"
            size="md"
            icon={Play}
            isLoading={isScanning}
            onClick={handleStartScan}
            className="bg-indigo-600 hover:bg-indigo-700 shadow-sm"
          >
            {isScanning ? 'Scan In Progress...' : 'Start New Scan'}
          </Button>
        }
      />

      {/* Active Scan Progress Section */}
      {isScanning && (
        <Card className="border-indigo-200 bg-gradient-to-r from-indigo-50/50 via-white to-violet-50/50 shadow-md">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-indigo-600 text-white flex items-center justify-center animate-pulse">
                  <ScanSearch className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-slate-900">
                    Scanning Target: {selectedTarget}
                  </h4>
                  <p className="text-xs text-indigo-700 mt-0.5">
                    Analyzing file descriptors, computing partial hashes & blur scores...
                  </p>
                </div>
              </div>
              <Badge variant="indigo" size="sm">
                {scanProgress}% Completed
              </Badge>
            </div>

            <ProgressBar
              value={scanProgress}
              max={100}
              color="gradient"
              size="md"
            />

            {/* Simulated Live Terminal Output */}
            <div className="p-3 bg-slate-950 text-slate-300 font-mono text-[11px] rounded-lg space-y-1 overflow-x-auto max-h-36">
              <p className="text-indigo-400">[INFO] Worker thread pool spawned (8 workers)</p>
              <p className="text-slate-400">[DEBUG] Traversing folder tree: C:/Users/rocky/Downloads</p>
              {scanProgress >= 30 && (
                <p className="text-amber-400">[MATCH] Exact SHA-256 duplicate identified: docker_desktop_installer_v4.28.exe</p>
              )}
              {scanProgress >= 60 && (
                <p className="text-violet-400">[BLUR] Laplacian variance 0.38 &lt; 0.85: DSC_0492_raw_burst_shot.jpg</p>
              )}
              {scanProgress >= 90 && (
                <p className="text-emerald-400">[SAVINGS] Aggregating recommendation manifest...</p>
              )}
            </div>
          </div>
        </Card>
      )}

      {/* Scanner Configuration Controls */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Target & Passes Selector */}
        <Card
          title="Scan Configuration & Scope"
          subtitle="Select directories and diagnostic algorithms"
          className="lg:col-span-2"
        >
          <div className="space-y-5">
            {/* Target Select */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Storage Target
              </label>
              <select
                value={selectedTarget}
                onChange={(e) => setSelectedTarget(e.target.value)}
                className="w-full text-xs bg-slate-50 border border-slate-200 rounded-lg p-2.5 text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="Local Computer (Downloads & Videos)">
                  Local Computer (Downloads & Videos) — 3 Paths
                </option>
                <option value="Local Computer (Projects/Archive)">
                  Local Computer (Projects/Archive) — 1 Path
                </option>
                <option value="Google Drive (Requires OAuth)">
                  Google Drive (Requires OAuth Integration)
                </option>
              </select>
            </div>

            {/* Diagnostic Passes */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Diagnostic Modules
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
                      Duplicate File Detection
                    </span>
                    <span className="text-[11px] text-slate-500">
                      Byte-for-byte MD5 & SHA-256 cryptographic match.
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
                      Laplacian edge variance detection &lt; 0.85 threshold.
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
                      Surfaces items exceeding 500 MB footprint.
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
                      Flags files unaccessed for over 180 days.
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
                className="w-full sm:w-auto"
              >
                Execute Scan Run
              </Button>
            </div>
          </div>
        </Card>

        {/* Engine Specs */}
        <Card
          title="Heuristic Engine Details"
          subtitle="High-performance local worker thread specs"
        >
          <div className="space-y-3 text-xs">
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Hashing Algorithm</span>
              <span className="font-semibold text-slate-800 font-mono">MD5 / SHA-256</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Blur Algorithm</span>
              <span className="font-semibold text-slate-800">Laplacian Variance</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Large File Threshold</span>
              <span className="font-semibold text-slate-800">500 MB</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">Inactivity Cutoff</span>
              <span className="font-semibold text-slate-800">180 Days</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-500 font-medium">IO Speed Rating</span>
              <span className="font-semibold text-emerald-600">~1,800 files/sec</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Audit Log of Past Scans */}
      <Card
        title="Historical Scan Executions"
        subtitle="Complete record of past diagnostic runs and identified waste"
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
                  <td className="py-3 px-3 font-mono">{scan.duration}</td>
                  <td className="py-3 px-3 font-semibold">
                    {formatNumber(scan.filesScanned)}
                  </td>
                  <td className="py-3 px-3 text-indigo-600 font-semibold">
                    {formatNumber(scan.flaggedFilesCount)}
                  </td>
                  <td className="py-3 px-3 font-bold text-slate-800">
                    {formatBytes(scan.potentialSavingsBytes)}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <Badge
                      variant={scan.status === 'completed' ? 'success' : 'default'}
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

