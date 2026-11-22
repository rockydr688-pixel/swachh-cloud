import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import Modal from '../components/common/Modal';
import { scanService } from '../services/scanService';
import { fileService } from '../services/fileService';
import { formatBytes, formatDate, formatNumber } from '../utils/formatters';
import {
  Clock,
  Sparkles,
  HardDrive,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  RefreshCw,
  ArrowRight,
  ShieldCheck,
  Search,
  ExternalLink,
} from 'lucide-react';

export const CleanupHistory = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('scans'); // 'scans' | 'cleanups'
  const [scans, setScans] = useState([]);
  const [cleanups, setCleanups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [restoringId, setRestoringId] = useState(null);
  const [notification, setNotification] = useState(null);
  const [selectedScanReport, setSelectedScanReport] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [scansRes, cleanupsRes] = await Promise.all([
        scanService.getRecentScans(1, 20),
        fileService.getCleanupHistory(1, 20),
      ]);
      setScans(scansRes.data || []);
      setCleanups(cleanupsRes.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleRestore = async (id) => {
    setRestoringId(id);
    try {
      const res = await fileService.restoreCleanup(id);
      setNotification({
        type: 'success',
        message: res.message || 'Files successfully restored to their original location.',
      });
      fetchData();
    } catch (e) {
      setNotification({
        type: 'error',
        message: e.message || 'Restoration failed.',
      });
    } finally {
      setRestoringId(null);
      setTimeout(() => setNotification(null), 6000);
    }
  };

  return (
    <div className="space-y-6 font-sans">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
              Audit & Activity
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Scan History
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Review previous storage scans and track restored or cleaned files.
          </p>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <Button
            variant="outline"
            size="sm"
            icon={RefreshCw}
            onClick={fetchData}
            className="text-xs border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Refresh
          </Button>
        </div>
      </div>

      {/* Notification */}
      {notification && (
        <div
          className={`p-4 rounded-xl border text-xs flex items-center justify-between ${
            notification.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          <div className="flex items-center gap-2 font-semibold">
            {notification.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            ) : (
              <AlertTriangle className="w-4 h-4 text-rose-600" />
            )}
            <span>{notification.message}</span>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-6">
        <button
          onClick={() => setActiveTab('scans')}
          className={`pb-3 text-sm font-bold border-b-2 transition-colors ${
            activeTab === 'scans'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          Previous Scans ({scans.length})
        </button>
        <button
          onClick={() => setActiveTab('cleanups')}
          className={`pb-3 text-sm font-bold border-b-2 transition-colors ${
            activeTab === 'cleanups'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          Cleanup & Recovery History ({cleanups.length})
        </button>
      </div>

      {/* Tab 1: Scans List */}
      {activeTab === 'scans' && (
        <div className="space-y-3">
          {scans.length === 0 ? (
            <div className="text-center py-16 px-6 bg-white rounded-2xl border border-slate-200 shadow-sm space-y-3">
              <Clock className="w-10 h-10 mx-auto text-slate-300" />
              <h3 className="text-base font-bold text-slate-900">No previous scans found</h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Run a scan from the Home or My Storage page to begin organizing your drives.
              </p>
              <Button
                variant="primary"
                size="sm"
                onClick={() => navigate('/dashboard')}
                className="bg-blue-600 hover:bg-blue-700 text-white font-medium"
              >
                Go to Home
              </Button>
            </div>
          ) : (
            scans.map((scan) => {
              const filesScanned = scan.filesScanned ?? scan.totalFilesScanned ?? 0;
              const junkBytes = scan.potentialSavingsBytes ?? scan.junkBytes ?? scan.bytesScanned ?? 0;
              const isCompleted = scan.status === 'completed' || scan.status === 'COMPLETED';

              return (
                <div
                  key={scan.id}
                  className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-sm hover:border-slate-300 transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="space-y-1.5 flex-1">
                    <div className="flex items-center gap-2.5">
                      <span className="font-bold text-sm text-slate-900">
                        {scan.target || 'Storage Drive'}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          isCompleted
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                            : scan.status === 'cancelled'
                            ? 'bg-slate-100 text-slate-600'
                            : 'bg-blue-50 text-blue-700'
                        }`}
                      >
                        {scan.status?.toUpperCase() || 'COMPLETED'}
                      </span>
                    </div>

                    <div className="flex items-center gap-4 text-xs text-slate-500 flex-wrap">
                      <span>
                        Date: <strong className="text-slate-700">{formatDate(scan.completedTime || scan.startTime || scan.createdAt)}</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Files Scanned: <strong className="text-slate-700">{formatNumber(filesScanned)}</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Potential Junk Found: <strong className="text-emerald-600">{formatBytes(junkBytes)}</strong>
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setSelectedScanReport(scan)}
                      className="text-xs border-slate-200 text-slate-700 hover:bg-slate-50"
                    >
                      View Report
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      icon={ArrowRight}
                      onClick={() => navigate('/junk')}
                      className="text-xs bg-blue-600 hover:bg-blue-700 text-white"
                    >
                      Open Junk Files
                    </Button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Tab 2: Cleanup & Recovery History */}
      {activeTab === 'cleanups' && (
        <div className="space-y-3">
          {cleanups.length === 0 ? (
            <div className="text-center py-16 px-6 bg-white rounded-2xl border border-slate-200 shadow-sm space-y-3">
              <RotateCcw className="w-10 h-10 mx-auto text-slate-300" />
              <h3 className="text-base font-bold text-slate-900">No cleanups performed yet</h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                When you move unnecessary files to trash, you will be able to review and restore them here anytime.
              </p>
            </div>
          ) : (
            cleanups.map((item) => {
              const isRestored = item.isRestored;
              return (
                <div
                  key={item.id}
                  className={`p-5 rounded-2xl border shadow-sm transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${
                    isRestored
                      ? 'bg-slate-50 border-slate-200 opacity-75'
                      : 'bg-white border-slate-200/80 hover:border-slate-300'
                  }`}
                >
                  <div className="space-y-1.5 flex-1">
                    <div className="flex items-center gap-2.5">
                      <span className="font-bold text-sm text-slate-900">
                        {item.action || 'Cleaned Files'}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          isRestored
                            ? 'bg-slate-200 text-slate-700'
                            : 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                        }`}
                      >
                        {isRestored ? 'Restored' : 'Moved to Trash'}
                      </span>
                    </div>

                    <div className="flex items-center gap-4 text-xs text-slate-500 flex-wrap">
                      <span>
                        Cleaned on: <strong className="text-slate-700">{formatDate(item.createdAt)}</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Files Cleaned: <strong className="text-slate-700">{item.filesAffected || item.filesCleanedCount || 1}</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Storage Recovered: <strong className="text-emerald-600">{formatBytes(item.freedSpaceBytes || item.bytesRecovered || 0)}</strong>
                      </span>
                    </div>
                  </div>

                  <div className="shrink-0">
                    {!isRestored && (
                      <Button
                        variant="outline"
                        size="sm"
                        icon={RotateCcw}
                        isLoading={restoringId === item.id}
                        onClick={() => handleRestore(item.id)}
                        className="text-xs border-slate-200 text-slate-700 hover:bg-slate-50"
                      >
                        Restore Files
                      </Button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Scan Report Modal */}
      {selectedScanReport && (
        <Modal
          isOpen={!!selectedScanReport}
          onClose={() => setSelectedScanReport(null)}
          title="Scan Execution Report"
          subtitle={`Detailed audit log for scan on ${selectedScanReport.target || 'Storage'}`}
          maxWidth="max-w-lg"
          footer={
            <Button variant="ghost" size="sm" onClick={() => setSelectedScanReport(null)}>
              Close
            </Button>
          }
        >
          <div className="space-y-4 text-xs">
            <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">Scan ID:</span>
                <span className="font-mono text-slate-700">{selectedScanReport.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Storage Target:</span>
                <span className="font-bold text-slate-900">{selectedScanReport.target}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Execution Status:</span>
                <span className="font-semibold text-emerald-700 uppercase">{selectedScanReport.status}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Files Inspected:</span>
                <span className="font-bold text-slate-900">
                  {formatNumber(selectedScanReport.filesScanned || selectedScanReport.totalFilesScanned || 0)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Potential Reclaim:</span>
                <span className="font-bold text-emerald-600">
                  {formatBytes(selectedScanReport.potentialSavingsBytes || selectedScanReport.bytesScanned || 0)}
                </span>
              </div>
            </div>

            <Button
              variant="primary"
              size="sm"
              icon={ArrowRight}
              onClick={() => {
                setSelectedScanReport(null);
                navigate('/junk');
              }}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white"
            >
              Review Identified Junk Files
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default CleanupHistory;
