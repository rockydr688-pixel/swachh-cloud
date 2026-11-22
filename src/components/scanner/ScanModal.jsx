import React, { useState } from 'react';
import { useStorage } from '../../context/StorageContext';
import { useToast } from '../../context/ToastContext';
import { formatBytes, formatNumber } from '../../utils/formatters';
import {
  ScanSearch,
  CheckCircle2,
  AlertTriangle,
  StopCircle,
  HardDrive,
  Cloud,
  Sparkles,
  X,
  ArrowRight,
} from 'lucide-react';
import Button from '../common/Button';
import ProgressBar from '../common/ProgressBar';

export const ScanModal = ({ isOpen, onClose, defaultTarget }) => {
  if (!isOpen) return null;

  return <ScanModalContent onClose={onClose} defaultTarget={defaultTarget} />;
};

const ScanModalContent = ({ onClose, defaultTarget }) => {
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
  } catch (e) {
    // Graceful fallback for test environments without ToastProvider
  }

  const [selectedTarget, setSelectedTarget] = useState(defaultTarget || 'all');
  const [scanFinished, setScanFinished] = useState(false);

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
    setScanFinished(false);
    try {
      if (selectedTarget === 'all') {
        await runScan('All Connected Accounts');
      } else {
        const conn = (connections || []).find((c) => c.id === selectedTarget || c.name === selectedTarget);
        await runScan({
          target: conn ? conn.name : selectedTarget,
          storageType: conn ? conn.type : 'local',
          storageConnectionId: conn?.id,
        });
      }
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200/80 max-w-lg w-full overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
              <ScanSearch className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-slate-900">
                {isScanning ? 'Scanning Storage' : 'Scan My Storage'}
              </h3>
              <p className="text-xs text-slate-500">
                {isScanning
                  ? 'Analyzing files across your connected accounts'
                  : 'Select accounts and find storage you can reclaim'}
              </p>
            </div>
          </div>
          {!isScanning && (
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-slate-600 p-1 rounded-lg transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Modal Body */}
        <div className="p-6 space-y-6">
          {/* While Scanning */}
          {isScanning ? (
            <div className="space-y-6">
              {/* Status Message */}
              <div className="text-center py-2">
                <div className="w-12 h-12 mx-auto mb-3 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center animate-pulse">
                  <Sparkles className="w-6 h-6" />
                </div>
                <h4 className="text-base font-medium text-slate-900">
                  {getStatusMessage()}
                </h4>
                <p className="text-xs text-slate-500 mt-1">
                  {activeScan?.target || 'All Connected Accounts'}
                </p>
              </div>

              {/* Progress Bar */}
              <div className="space-y-2">
                <div className="flex justify-between text-xs font-medium text-slate-600">
                  <span>Progress</span>
                  <span className="text-blue-600 font-bold">{Math.round(scanProgress || 0)}%</span>
                </div>
                <ProgressBar value={scanProgress || 0} color="blue" size="md" />
              </div>

              {/* Live Metric Badges */}
              <div className="grid grid-cols-3 gap-3">
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center">
                  <div className="text-xs text-slate-500 font-medium">Discovered</div>
                  <div className="text-base font-bold text-slate-900 mt-0.5">
                    {formatNumber(activeScan?.totalFilesScanned || 0)}
                  </div>
                </div>
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center">
                  <div className="text-xs text-slate-500 font-medium">Analyzed</div>
                  <div className="text-base font-bold text-slate-900 mt-0.5">
                    {formatNumber(Math.round((activeScan?.totalFilesScanned || 0) * ((scanProgress || 5) / 100)))}
                  </div>
                </div>
                <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-100 text-center">
                  <div className="text-xs text-emerald-700 font-medium">Potential Junk</div>
                  <div className="text-base font-bold text-emerald-700 mt-0.5">
                    {formatBytes(activeScan?.potentialSavingsBytes || 0)}
                  </div>
                </div>
              </div>

              {/* Cancel Button */}
              <div className="pt-2">
                <Button
                  variant="outline"
                  size="md"
                  icon={StopCircle}
                  onClick={handleCancel}
                  className="w-full text-slate-600 border-slate-200 hover:bg-slate-50"
                >
                  Cancel Scan
                </Button>
              </div>
            </div>
          ) : (
            /* Configure and Start */
            <div className="space-y-5">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                  Target Storage
                </label>
                <div className="space-y-2">
                  <label className="flex items-center gap-3 p-3 rounded-xl border border-slate-200 hover:border-blue-300 hover:bg-blue-50/30 cursor-pointer transition-colors">
                    <input
                      type="radio"
                      name="scanTarget"
                      value="all"
                      checked={selectedTarget === 'all'}
                      onChange={(e) => setSelectedTarget(e.target.value)}
                      className="text-blue-600 focus:ring-blue-500"
                    />
                    <div className="flex-1">
                      <div className="text-sm font-semibold text-slate-900">All Connected Accounts</div>
                      <div className="text-xs text-slate-500">Scan across all local and cloud storage simultaneously</div>
                    </div>
                  </label>

                  {(connections || []).map((conn) => (
                    <label
                      key={conn.id}
                      className="flex items-center gap-3 p-3 rounded-xl border border-slate-200 hover:border-blue-300 hover:bg-blue-50/30 cursor-pointer transition-colors"
                    >
                      <input
                        type="radio"
                        name="scanTarget"
                        value={conn.id}
                        checked={selectedTarget === conn.id}
                        onChange={(e) => setSelectedTarget(e.target.value)}
                        className="text-blue-600 focus:ring-blue-500"
                      />
                      <div className="flex-1">
                        <div className="text-sm font-semibold text-slate-900">{conn.name}</div>
                        <div className="text-xs text-slate-500 capitalize">{conn.type?.replace('_', ' ')} · {conn.status}</div>
                      </div>
                    </label>
                  ))}
                </div>
              </div>

              {/* What will be detected */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-xs text-slate-600 space-y-1">
                <div className="font-semibold text-slate-800">What will be analyzed:</div>
                <div className="flex flex-wrap gap-2 pt-1 text-slate-700">
                  <span className="px-2 py-0.5 bg-white rounded border border-slate-200">Duplicates</span>
                  <span className="px-2 py-0.5 bg-white rounded border border-slate-200">Blurry Photos</span>
                  <span className="px-2 py-0.5 bg-white rounded border border-slate-200">Old Files</span>
                  <span className="px-2 py-0.5 bg-white rounded border border-slate-200">Large Files</span>
                  <span className="px-2 py-0.5 bg-white rounded border border-slate-200">Temporary Files</span>
                </div>
              </div>

              {/* Start Scan Button */}
              <div className="flex gap-3 pt-2">
                <Button
                  variant="outline"
                  size="md"
                  onClick={onClose}
                  className="flex-1 border-slate-200 text-slate-600"
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  icon={ScanSearch}
                  onClick={handleStartScan}
                  className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-medium"
                >
                  Start Scan
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ScanModal;
