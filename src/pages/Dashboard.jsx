import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  HardDrive,
  Sparkles,
  RefreshCw,
  FolderOpen,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Clock,
  Trash2,
  ExternalLink,
  ChevronRight,
  FileCheck2,
} from 'lucide-react';
import { useStorage } from '../context/StorageContext';
import { formatBytes, formatNumber, formatDate } from '../utils/formatters';
import Button from '../components/common/Button';
import Card from '../components/common/Card';
import Badge from '../components/common/Badge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ScanModal from '../components/scanner/ScanModal';

export const Dashboard = () => {
  const navigate = useNavigate();
  const { stats, connections, recentScans, loading, runScan, isScanning } = useStorage();
  const [scanModalOpen, setScanModalOpen] = useState(false);

  if (loading || !stats) {
    return <LoadingSpinner fullPage message="Aggregating storage telemetry..." />;
  }

  const usedPercentage = Math.round(
    ((stats.totalUsedBytes || 0) / (stats.totalCapacityBytes || 1)) * 100
  );

  // Group or identify standard 4 providers
  const providersList = [
    {
      type: 'local',
      displayName: 'Local Computer',
      iconUrl: null,
      defaultAccount: 'My Workstation',
    },
    {
      type: 'google_drive',
      displayName: 'Google Drive',
      iconUrl: null,
      defaultAccount: 'Google Account',
    },
    {
      type: 'onedrive',
      displayName: 'OneDrive',
      iconUrl: null,
      defaultAccount: 'Microsoft Account',
    },
    {
      type: 'dropbox',
      displayName: 'Dropbox',
      iconUrl: null,
      defaultAccount: 'Dropbox Account',
    },
  ];

  const getMatchedConnection = (providerType) => {
    return (connections || []).find(
      (c) => c.type === providerType || c.type?.toLowerCase().includes(providerType)
    );
  };

  return (
    <div className="space-y-8 font-sans">
      {/* 1. Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-2 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
              Storage Intelligence Dashboard
            </span>
            <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-slate-100 text-slate-600">
              AI Engine v2.4
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Your cloud storage, cleaned and organized.
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Find duplicates, blurry photos, and old files to reclaim space across your accounts.
          </p>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCw}
            onClick={() => runScan('Quick Scan')}
            isLoading={isScanning}
            className="text-xs border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Refresh Telemetry
          </Button>
          <Button
            variant="outline"
            size="sm"
            icon={Sparkles}
            onClick={() => navigate('/junk')}
            className="text-xs border-blue-200 text-blue-600 hover:bg-blue-50"
          >
            Review Recommendations
          </Button>
        </div>
      </div>

      {/* Empty State when no files scanned yet */}
      {stats.totalFiles === 0 ? (
        <div className="text-center py-16 px-6 rounded-2xl border-2 border-dashed border-slate-200 bg-white">
          <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
            <HardDrive className="w-8 h-8 opacity-90" />
          </div>
          <h3 className="text-lg font-bold text-slate-900">
            No files scanned yet. Connect your storage to get started.
          </h3>
          <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto mt-1.5 mb-6 leading-relaxed">
            Link a local computer directory or authorize your cloud accounts to find unnecessary files and reclaim gigabytes of storage.
          </p>
          <div className="flex items-center justify-center gap-3 flex-wrap">
            <Button
              variant="outline"
              icon={FolderOpen}
              onClick={() => navigate('/storage')}
              className="text-slate-700 border-slate-200"
            >
              Connect Storage
            </Button>
            <Button
              variant="primary"
              icon={Sparkles}
              onClick={() => setScanModalOpen(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              Start First Scan
            </Button>
          </div>
        </div>
      ) : (
        <>
          {/* 2. Top 3 Important Statistics Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* Stat 1: Total Storage Used */}
            <div className="p-6 bg-white rounded-2xl border border-slate-200/80 shadow-sm space-y-3">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <span>Total Storage Used</span>
                <span className="text-slate-400 font-normal">Total Storage Pool Capacity</span>
              </div>
              <div className="text-3xl font-extrabold text-slate-900 tracking-tight">
                {formatBytes(stats.totalUsedBytes || 0)}
              </div>
              <div className="space-y-1.5 pt-1">
                <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-blue-600 rounded-full transition-all duration-300"
                    style={{ width: `${Math.min(100, usedPercentage || 45)}%` }}
                  />
                </div>
                <div className="flex justify-between text-[11px] text-slate-500">
                  <span>{usedPercentage}% capacity used</span>
                  <span>{formatBytes(stats.totalCapacityBytes - stats.totalUsedBytes)} free</span>
                </div>
              </div>
            </div>

            {/* Stat 2: Potential Space to Recover */}
            <div className="p-6 bg-white rounded-2xl border border-slate-200/80 shadow-sm space-y-3">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <span>Potential Space to Recover</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[11px] font-bold">
                  Recoverable Savings
                </span>
              </div>
              <div className="text-3xl font-extrabold text-emerald-600 tracking-tight">
                {formatBytes(stats.recoverableBytes || 0)}
              </div>
              <div className="text-xs text-slate-500 pt-1 flex items-center justify-between">
                <span>Safe to clean without losing originals</span>
                <button
                  onClick={() => navigate('/junk')}
                  className="text-blue-600 hover:text-blue-700 font-semibold flex items-center gap-1"
                >
                  Clean Now <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Stat 3: Files Scanned */}
            <div className="p-6 bg-white rounded-2xl border border-slate-200/80 shadow-sm space-y-3">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <span>Files Scanned</span>
                <span className="text-xs text-slate-400">Total Catalog</span>
              </div>
              <div className="text-3xl font-extrabold text-slate-900 tracking-tight">
                {formatNumber(stats.totalFiles || 0)}
              </div>
              <div className="pt-1 text-[11px] text-slate-500 space-y-0.5">
                <div className="flex justify-between">
                  <span>Exact Duplicates:</span>
                  <span className="font-semibold text-slate-700">{formatNumber(stats.duplicateCount || 0)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Blurry Photos:</span>
                  <span className="font-semibold text-slate-700">{formatNumber(stats.blurryImagesCount || 0)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Stale Large Files:</span>
                  <span className="font-semibold text-slate-700">{formatNumber(stats.largeFilesCount || 0)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* 3. Primary CTA Banner: Scan My Storage */}
          <div className="p-6 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white flex flex-col sm:flex-row items-center justify-between gap-4 shadow-sm">
            <div className="space-y-1 text-center sm:text-left">
              <h2 className="text-xl font-bold">Ready to free up storage space?</h2>
              <p className="text-sm text-blue-100 max-w-xl">
                Start a quick scan to detect duplicate files, blurry camera shots, and old downloads across all your connected drives.
              </p>
            </div>
            <button
              onClick={() => setScanModalOpen(true)}
              className="px-6 py-3.5 rounded-xl bg-white text-blue-700 hover:bg-blue-50 font-bold text-sm shadow-md transition-all shrink-0 flex items-center gap-2 hover:scale-[1.02] active:scale-[0.98]"
            >
              <Sparkles className="w-4 h-4 text-blue-600" />
              <span>Scan My Storage</span>
            </button>
          </div>

          {/* 4. Connected Accounts Section */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Connected Accounts</h3>
                <p className="text-xs text-slate-500">Manage and monitor storage across your devices and cloud drives</p>
              </div>
              <button
                onClick={() => navigate('/storage')}
                className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"
              >
                Manage Storage <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {providersList.map((provider) => {
                const conn = getMatchedConnection(provider.type);
                const isConnected = conn && conn.status === 'connected';

                return (
                  <div
                    key={provider.type}
                    className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-sm flex flex-col justify-between hover:border-slate-300 transition-colors"
                  >
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="w-9 h-9 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-center font-bold text-slate-700">
                          {provider.type === 'google_drive' && <span className="text-amber-500 text-sm font-black">G</span>}
                          {provider.type === 'onedrive' && <span className="text-blue-500 text-sm font-black">M</span>}
                          {provider.type === 'dropbox' && <span className="text-sky-600 text-sm font-black">D</span>}
                          {provider.type === 'local' && <HardDrive className="w-4 h-4 text-slate-600" />}
                        </div>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                            isConnected
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                              : 'bg-slate-100 text-slate-500'
                          }`}
                        >
                          {isConnected ? 'Connected' : 'Not Connected'}
                        </span>
                      </div>

                      <div>
                        <h4 className="text-sm font-bold text-slate-900">{provider.displayName}</h4>
                        <p className="text-xs text-slate-400 truncate">
                          {conn?.name || conn?.accountEmail || provider.defaultAccount}
                        </p>
                      </div>

                      {isConnected && conn?.usedSpaceBytes ? (
                        <div className="text-xs text-slate-500">
                          <span className="font-semibold text-slate-800">{formatBytes(conn.usedSpaceBytes)}</span> used
                        </div>
                      ) : (
                        <div className="text-xs text-slate-400">Ready to link</div>
                      )}
                    </div>

                    <div className="pt-4 border-t border-slate-100 mt-4 flex items-center justify-between">
                      {isConnected ? (
                        <button
                          onClick={() => {
                            runScan({
                              target: conn.name,
                              storageType: conn.type,
                              storageConnectionId: conn.id,
                            });
                          }}
                          disabled={isScanning}
                          className="text-xs font-semibold text-blue-600 hover:text-blue-700 transition-colors"
                        >
                          Scan Now
                        </button>
                      ) : (
                        <button
                          onClick={() => navigate('/storage')}
                          className="text-xs font-semibold text-slate-600 hover:text-slate-900 transition-colors"
                        >
                          Connect
                        </button>
                      )}
                      <span className="text-[11px] text-slate-400">
                        {conn?.lastScannedAt ? formatDate(conn.lastScannedAt) : 'Never scanned'}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 5. Small Recent Activity Section */}
          <div className="p-6 bg-white rounded-2xl border border-slate-200/80 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900">Recent Activity</h3>
                <p className="text-xs text-slate-500">Latest scans and storage operations</p>
              </div>
              <button
                onClick={() => navigate('/history')}
                className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"
              >
                View All History <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="divide-y divide-slate-100">
              {recentScans && recentScans.length > 0 ? (
                recentScans.slice(0, 4).map((scan, idx) => (
                  <div key={scan.id || idx} className="py-3 flex items-center justify-between text-xs">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-slate-50 text-slate-600 flex items-center justify-center">
                        <Clock className="w-4 h-4 text-slate-400" />
                      </div>
                      <div>
                        <div className="font-semibold text-slate-800">
                          Scan completed on {scan.target || 'Storage'}
                        </div>
                        <div className="text-slate-400 text-[11px]">
                          {scan.filesScanned || scan.totalFilesScanned || 0} files inspected ·{' '}
                          {formatBytes(scan.bytesScanned || scan.totalBytesScanned || 0)}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700">
                        Completed
                      </span>
                      <div className="text-[10px] text-slate-400 mt-1">
                        {scan.completedTime || scan.startTime ? formatDate(scan.completedTime || scan.startTime) : 'Recent'}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="py-6 text-center text-xs text-slate-400">
                  No scan activity recorded yet. Run a scan to see recent history.
                </div>
              )}
            </div>
          </div>
        </>
      )}

      {/* Global Scan Modal */}
      {scanModalOpen && (
        <ScanModal
          isOpen={scanModalOpen}
          onClose={() => setScanModalOpen(false)}
        />
      )}
    </div>
  );
};

export default Dashboard;
