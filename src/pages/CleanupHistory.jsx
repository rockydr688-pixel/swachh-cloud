import React, { useState, useEffect } from 'react';
import PageHeader from '../components/layout/PageHeader';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import SearchInput from '../components/common/SearchInput';
import Modal from '../components/common/Modal';
import { fileService } from '../services/fileService';
import { formatBytes, formatDateTime, formatNumber } from '../utils/formatters';
import {
  History,
  Download,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  XCircle,
  Info,
  ShieldCheck,
  RefreshCw,
} from 'lucide-react';

export const CleanupHistory = () => {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterCategory, setFilterCategory] = useState('all');
  const [restoringId, setRestoringId] = useState(null);
  const [notification, setNotification] = useState(null);

  // Error details modal
  const [errorModalOpen, setErrorModalOpen] = useState(false);
  const [selectedErrorItem, setSelectedErrorItem] = useState(null);

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const res = await fileService.getCleanupHistory(1, 50);
      setHistory(res.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleRestore = async (id) => {
    setRestoringId(id);
    try {
      const res = await fileService.restoreCleanup(id);
      setNotification({
        type: 'success',
        message: res.message || 'Files successfully restored to original location.',
      });
      fetchHistory();
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

  const filteredHistory = history.filter((item) => {
    const matchCategory =
      filterCategory === 'all' ||
      (item.targetCategory && item.targetCategory.toLowerCase() === filterCategory.toLowerCase());
    const matchSearch =
      !search ||
      (item.action && item.action.toLowerCase().includes(search.toLowerCase())) ||
      (item.executedBy && item.executedBy.toLowerCase().includes(search.toLowerCase())) ||
      (item.method && item.method.toLowerCase().includes(search.toLowerCase()));
    return matchCategory && matchSearch;
  });

  const totalFreed = history.filter(h => !h.isRestored).reduce((sum, h) => sum + (h.freedSpaceBytes || 0), 0);
  const totalFiles = history.reduce((sum, h) => sum + (h.filesAffected || 0), 0);

  const handleExportCSV = async () => {
    await fileService.exportCsv('cleanup');
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Cleanup Execution History & Recovery Center"
        subtitle="Cryptographically verified audit trail of purged items with atomic undo and restoration support."
        breadcrumbs={[{ label: 'Cleanup History' }]}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              icon={RefreshCw}
              onClick={fetchHistory}
            >
              Refresh
            </Button>
            <Button
              variant="secondary"
              size="sm"
              icon={Download}
              onClick={handleExportCSV}
            >
              Export Audit CSV
            </Button>
          </div>
        }
      />

      {/* Alert Notification */}
      {notification && (
        <div
          className={`p-4 rounded-xl border text-xs flex items-center justify-between ${
            notification.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-900'
              : 'bg-emerald-50 border-emerald-200 text-emerald-900'
          }`}
        >
          <div className="flex items-center gap-2 font-semibold">
            {notification.type === 'error' ? (
              <XCircle className="w-4 h-4 text-rose-600" />
            ) : (
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            )}
            <span>{notification.message}</span>
          </div>
          <Badge variant={notification.type === 'error' ? 'danger' : 'success'} size="sm">
            {notification.type === 'error' ? 'Action Failed' : 'Action Confirmed'}
          </Badge>
        </div>
      )}

      {/* Aggregate Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-emerald-50/50 border-emerald-200">
          <span className="text-xs text-emerald-700 font-semibold uppercase">
            Net Storage Reclaimed
          </span>
          <h4 className="text-2xl font-bold text-slate-900 mt-1">
            {formatBytes(totalFreed)}
          </h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Excludes restored file batches</p>
        </Card>

        <Card className="bg-indigo-50/50 border-indigo-200">
          <span className="text-xs text-indigo-700 font-semibold uppercase">
            Total Files Cleaned
          </span>
          <h4 className="text-2xl font-bold text-slate-900 mt-1">
            {formatNumber(totalFiles)} Files
          </h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Staged in recycle bin for safe recovery</p>
        </Card>

        <Card className="bg-violet-50/50 border-violet-200">
          <span className="text-xs text-violet-700 font-semibold uppercase">
            Restoration Availability
          </span>
          <h4 className="text-2xl font-bold text-slate-900 mt-1">Provider Sandboxed</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Local & Google Drive Trash supported</p>
        </Card>
      </div>

      {/* Filter and Search */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="w-full sm:w-80">
          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Search audit actions, operators..."
          />
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500 font-medium">Category:</span>
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-700 font-medium"
          >
            <option value="all">All Categories</option>
            <option value="Duplicates">Duplicates</option>
            <option value="Blurry Images">Blurry Images</option>
            <option value="Large Files">Large Files</option>
            <option value="Inactive Files">Inactive Files</option>
          </select>
        </div>
      </div>

      {/* History Table */}
      <Card noPadding>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                <th className="py-3 px-4">Action & Operation</th>
                <th className="py-3 px-3">Category</th>
                <th className="py-3 px-3">Date & Time</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-3">Files</th>
                <th className="py-3 px-3">Freed Space</th>
                <th className="py-3 px-3">Method</th>
                <th className="py-3 px-4 text-right">Recovery Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {filteredHistory.map((item) => {
                const isFailed = item.status === 'failed' || (item.failedCount && item.failedCount > 0);

                return (
                  <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="py-3.5 px-4 font-semibold text-slate-900">
                      <div className="flex items-center gap-2">
                        {item.isRestored ? (
                          <RotateCcw className="w-4 h-4 text-indigo-500 shrink-0" />
                        ) : item.status === 'failed' ? (
                          <XCircle className="w-4 h-4 text-rose-500 shrink-0" />
                        ) : (
                          <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
                        )}
                        <span>{item.action}</span>
                      </div>
                    </td>

                    <td className="py-3.5 px-3">
                      <Badge variant="navy" size="xs">
                        {item.targetCategory || 'General'}
                      </Badge>
                    </td>

                    <td className="py-3.5 px-3 text-slate-500 whitespace-nowrap">
                      {formatDateTime(item.timestamp)}
                    </td>

                    <td className="py-3.5 px-3">
                      {item.isRestored ? (
                        <Badge variant="neutral" size="xs">
                          Restored
                        </Badge>
                      ) : item.status === 'failed' ? (
                        <button
                          onClick={() => {
                            setSelectedErrorItem(item);
                            setErrorModalOpen(true);
                          }}
                          className="hover:underline flex items-center gap-1"
                        >
                          <Badge variant="danger" size="xs">
                            Failed ({item.failedCount || 1})
                          </Badge>
                        </button>
                      ) : item.status === 'partial' ? (
                        <button
                          onClick={() => {
                            setSelectedErrorItem(item);
                            setErrorModalOpen(true);
                          }}
                          className="hover:underline flex items-center gap-1"
                        >
                          <Badge variant="warning" size="xs">
                            Partial ({item.failedCount} errors)
                          </Badge>
                        </button>
                      ) : (
                        <Badge variant="success" size="xs">
                          Success
                        </Badge>
                      )}
                    </td>

                    <td className="py-3.5 px-3 font-semibold">
                      {formatNumber(item.filesAffected)}
                    </td>

                    <td className="py-3.5 px-3 font-bold text-emerald-600 whitespace-nowrap">
                      {formatBytes(item.freedSpaceBytes)}
                    </td>

                    <td className="py-3.5 px-3 font-mono text-[11px] text-slate-600">
                      {item.method || 'Safe Trash'}
                    </td>

                    {/* Recovery / Restoration Button */}
                    <td className="py-3.5 px-4 text-right">
                      {item.isRestored ? (
                        <span className="text-[11px] text-slate-400 font-medium">
                          Restored {item.restoredAt ? formatDateTime(item.restoredAt) : ''}
                        </span>
                      ) : item.restorationSupported !== false ? (
                        <Button
                          size="xs"
                          variant="outline"
                          icon={RotateCcw}
                          isLoading={restoringId === item.id}
                          onClick={() => handleRestore(item.id)}
                          title="Restore files back to original folder"
                        >
                          Restore
                        </Button>
                      ) : (
                        <span
                          className="text-[10px] text-slate-400 italic"
                          title="Restoration is unavailable for this storage target (e.g. permanent cold archive)"
                        >
                          Restoration Unavailable
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Error Details Modal for Failed / Partial Cleanups */}
      <Modal
        isOpen={errorModalOpen}
        onClose={() => setErrorModalOpen(false)}
        title="Cleanup Diagnostics & Error Report"
        subtitle="Failure logs reported during provider trash operations"
        maxWidth="max-w-md"
        footer={
          <Button variant="ghost" size="sm" onClick={() => setErrorModalOpen(false)}>
            Close
          </Button>
        }
      >
        {selectedErrorItem && (
          <div className="space-y-3 text-xs">
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-900">
              <span className="font-bold block">
                Operation: {selectedErrorItem.action}
              </span>
              <p className="mt-1">
                Failed to move {selectedErrorItem.failedCount} file(s) to trash.
              </p>
            </div>

            {selectedErrorItem.errorMessages && selectedErrorItem.errorMessages.length > 0 && (
              <div className="space-y-1.5">
                <span className="font-bold text-slate-800 uppercase text-[11px]">
                  Provider Error Messages:
                </span>
                <ul className="list-disc list-inside space-y-1 text-slate-600 bg-slate-50 p-3 rounded-lg border border-slate-200">
                  {selectedErrorItem.errorMessages.map((msg, i) => (
                    <li key={i}>{msg}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default CleanupHistory;
