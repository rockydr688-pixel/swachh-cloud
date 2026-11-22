import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import ProgressBar from '../components/common/ProgressBar';
import { useStorage } from '../context/StorageContext';
import { useToast } from '../context/ToastContext';
import { formatBytes, formatDate, formatNumber } from '../utils/formatters';
import {
  ScanSearch,
  Sparkles,
  RotateCcw,
  CheckCircle2,
  Clock,
  HardDrive,
  Cloud,
  StopCircle,
  AlertTriangle,
  ArrowRight,
  Layers,
  Box,
} from 'lucide-react';

export const FileScanner = () => {
  const navigate = useNavigate();
  const {
    connections = [],
    isScanning,
    scanProgress,
    activeScan,
    scanError,
    runScan,
    cancelActiveScan,
  } = useStorage();

  let addToast = () => {};
  try {
    const toast = useToast();
    if (toast && toast.addToast) addToast = toast.addToast;
  } catch (e) {}

  const [selectedTarget, setSelectedTarget] = useState('all');

  const getStatusMessage = () => {
    if (!activeScan) return 'Connecting to your storage...';
    const percent = scanProgress || 0;
    if (percent < 20) return 'Connecting to your storage...';
    if (percent < 45) return 'Finding duplicate files...';
    if (percent < 65) return 'Checking image quality...';
    if (percent < 85) return 'Analyzing large files...';
    return 'Preparing recommendations...';
  };

  const handleStartScan = async () => {
    try {
      if (selectedTarget === 'all') {
        await runScan('All Connected Accounts');
      } else {
        const conn = connections.find((c) => c.id === selectedTarget || c.name === selectedTarget);
        await runScan({
          target: conn ? conn.name : selectedTarget,
          storageType: conn ? conn.type : 'local',
          storageConnectionId: conn?.id,
        });
      }
      addToast({
        title: 'Scan Started',
        message: 'Analyzing files across your selected storage.',
        type: 'info',
      });
    } catch (err) {
      addToast({
        title: 'Scan Failed',
        message: err.message || 'Could not start scan.',
        type: 'error',
      });
    }
  };

  const handleCancel = async () => {
    if (cancelActiveScan) {
      await cancelActiveScan();
      addToast({
        title: 'Scan Cancelled',
        message: 'The scan was stopped.',
        type: 'info',
      });
    }
  };

  return (
    <div className="space-y-6 font-sans max-w-4xl">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
              Diagnostic Scanner
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Scan My Storage
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Analyze your files to uncover duplicates, blurry photos, and large storage waste.
          </p>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          {isScanning ? (
            <Button
              variant="outline"
              size="sm"
              icon={StopCircle}
              onClick={handleCancel}
              className="text-xs text-rose-600 border-rose-200 hover:bg-rose-50"
            >
              Cancel Scan
            </Button>
          ) : (
            <Button
              variant="primary"
              size="md"
              icon={ScanSearch}
              onClick={handleStartScan}
              className="bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-sm"
            >
              Start Scan
            </Button>
          )}
        </div>
      </div>

      {/* Error Alert */}
      {scanError && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
          <div>
            <h4 className="font-bold text-rose-950 text-sm">Scan Notification</h4>
            <p>{scanError}</p>
          </div>
        </div>
      )}

      {/* Live Scanning View */}
      {isScanning && (
        <div className="p-6 bg-white rounded-2xl border border-blue-200 shadow-sm space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center animate-pulse">
                <Sparkles className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">{getStatusMessage()}</h3>
                <p className="text-xs text-slate-500">{activeScan?.target || 'All Connected Accounts'}</p>
              </div>
            </div>
            <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700">
              {Math.round(scanProgress || 0)}%
            </span>
          </div>

          <ProgressBar value={scanProgress || 0} color="blue" size="md" />

          {/* Real Metrics Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-1">
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <div className="text-xs text-slate-500 font-medium">Files Discovered</div>
              <div className="text-xl font-extrabold text-slate-900 mt-1">
                {formatNumber(activeScan?.totalFilesScanned || 0)}
              </div>
            </div>

            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <div className="text-xs text-slate-500 font-medium">Files Analyzed</div>
              <div className="text-xl font-extrabold text-slate-900 mt-1">
                {formatNumber(Math.round((activeScan?.totalFilesScanned || 0) * ((scanProgress || 5) / 100)))}
              </div>
            </div>

            <div className="p-4 bg-emerald-50 rounded-xl border border-emerald-100">
              <div className="text-xs text-emerald-700 font-medium">Potential Junk Detected</div>
              <div className="text-xl font-extrabold text-emerald-700 mt-1">
                {formatBytes(activeScan?.potentialSavingsBytes || 0)}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Target Selector */}
      {!isScanning && (
        <div className="p-6 bg-white rounded-2xl border border-slate-200/80 shadow-sm space-y-5">
          <div>
            <h3 className="text-sm font-bold text-slate-900">Select Storage to Scan</h3>
            <p className="text-xs text-slate-500">Choose all accounts or an individual drive</p>
          </div>

          <div className="space-y-2.5">
            <label className="flex items-center gap-3.5 p-4 rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/20 cursor-pointer transition-all">
              <input
                type="radio"
                name="target"
                value="all"
                checked={selectedTarget === 'all'}
                onChange={(e) => setSelectedTarget(e.target.value)}
                className="text-blue-600 focus:ring-blue-500"
              />
              <div className="flex-1">
                <div className="text-sm font-bold text-slate-900">All Connected Accounts</div>
                <div className="text-xs text-slate-500">Scan across all your cloud drives and local computer directories</div>
              </div>
            </label>

            {connections.map((conn) => (
              <label
                key={conn.id}
                className="flex items-center gap-3.5 p-4 rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/20 cursor-pointer transition-all"
              >
                <input
                  type="radio"
                  name="target"
                  value={conn.id}
                  checked={selectedTarget === conn.id}
                  onChange={(e) => setSelectedTarget(e.target.value)}
                  className="text-blue-600 focus:ring-blue-500"
                />
                <div className="flex-1">
                  <div className="text-sm font-bold text-slate-900">{conn.name}</div>
                  <div className="text-xs text-slate-400 capitalize">{conn.type?.replace('_', ' ')} · {conn.status}</div>
                </div>
              </label>
            ))}
          </div>

          <div className="pt-2">
            <Button
              variant="primary"
              size="md"
              icon={ScanSearch}
              onClick={handleStartScan}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3"
            >
              Start Scanning
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default FileScanner;
