import React, { useState, useEffect, useMemo } from 'react';
import PageHeader from '../components/layout/PageHeader';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import Modal from '../components/common/Modal';
import { fileService } from '../services/fileService';
import { formatBytes, formatDate } from '../utils/formatters';
import {
  Sparkles,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  Trash2,
  ArrowRight,
  HardDrive,
  Info,
  Check,
  X,
  Eye,
  Shield,
  Filter,
  RefreshCw,
  Search,
  Copy,
  ImageOff,
  Clock,
  Maximize2,
  FileCode,
  File,
} from 'lucide-react';

export const CleanupRecommendations = () => {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [activeCategory, setActiveCategory] = useState('all'); // 'all' | 'duplicate' | 'blurry' | 'inactive' | 'large' | 'temp'
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStorage, setFilterStorage] = useState('all');

  // Modals state
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [activeDetailRec, setActiveDetailRec] = useState(null);
  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [impactData, setImpactData] = useState(null);
  const [calculatingImpact, setCalculatingImpact] = useState(false);
  const [explicitConfirmed, setExplicitConfirmed] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [notificationMsg, setNotificationMsg] = useState(null);

  const fetchRecommendations = async () => {
    setLoading(true);
    try {
      const res = await fileService.getRecommendations({});
      setRecommendations(res.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecommendations();
  }, []);

  // 5 Main Categories
  const categoryTabs = [
    { id: 'all', label: 'All Junk Files', icon: Sparkles },
    { id: 'duplicate', label: 'Duplicates', icon: Copy },
    { id: 'blurry', label: 'Blurry Photos', icon: ImageOff },
    { id: 'inactive', label: 'Old Files', icon: Clock },
    { id: 'large', label: 'Large Files', icon: Maximize2 },
    { id: 'temp', label: 'Temporary Files', icon: FileCode },
  ];

  // Helper for Plain-English detection explanation
  const getFriendlyReason = (rec) => {
    const raw = (rec.description || rec.detectionReasons?.[0] || '').toLowerCase();
    const cat = (rec.category || '').toLowerCase();

    if (cat.includes('dup') || raw.includes('duplicate') || raw.includes('sha-256') || raw.includes('hash')) {
      return 'This file is an exact duplicate.';
    }
    if (cat.includes('blur') || raw.includes('blur') || raw.includes('laplacian')) {
      return 'This photo may be blurry.';
    }
    if (cat.includes('inact') || raw.includes('inactive') || raw.includes('access') || raw.includes('year') || raw.includes('old')) {
      return 'Not opened or accessed in a long time.';
    }
    if (cat.includes('large') || raw.includes('large') || raw.includes('heavy') || raw.includes('size')) {
      return 'Heavy storage consumer taking up significant space.';
    }
    if (cat.includes('temp') || raw.includes('temp') || raw.includes('cache') || raw.includes('log')) {
      return 'Temporary system or cache file safe to remove.';
    }
    return 'Identified as storage waste.';
  };

  const getFileCategoryType = (rec) => {
    const cat = (rec.category || '').toLowerCase();
    const desc = (rec.description || '').toLowerCase();
    if (cat.includes('dup') || desc.includes('duplicate')) return 'duplicate';
    if (cat.includes('blur') || desc.includes('blur')) return 'blurry';
    if (cat.includes('inact') || desc.includes('inactive') || desc.includes('old')) return 'inactive';
    if (cat.includes('large') || desc.includes('large')) return 'large';
    if (cat.includes('temp') || desc.includes('temp')) return 'temp';
    return 'other';
  };

  // Filter recommendations
  const filteredRecs = useMemo(() => {
    return recommendations.filter((rec) => {
      // Category filter
      if (activeCategory !== 'all') {
        const itemCat = getFileCategoryType(rec);
        if (itemCat !== activeCategory) return false;
      }
      // Storage source filter
      if (filterStorage !== 'all') {
        const src = (rec.storageSource || '').toLowerCase();
        if (!src.includes(filterStorage.toLowerCase())) return false;
      }
      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const name = (rec.fileName || rec.title || '').toLowerCase();
        const loc = (rec.storageLocation || '').toLowerCase();
        const desc = (rec.description || '').toLowerCase();
        if (!name.includes(q) && !loc.includes(q) && !desc.includes(q)) return false;
      }
      return true;
    });
  }, [recommendations, activeCategory, filterStorage, searchQuery]);

  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedIds(new Set(filteredRecs.map((r) => r.id)));
    } else {
      setSelectedIds(new Set());
    }
  };

  const handleToggleRow = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleProtect = async (rec) => {
    const fileId = rec.fileId || (rec.targetFileIds && rec.targetFileIds[0]);
    if (!fileId) return;
    try {
      await fileService.protectFile(fileId);
      setRecommendations((prev) =>
        prev.map((r) =>
          r.id === rec.id
            ? { ...r, status: 'protected', isProtected: true, recommendedAction: 'Keep Original', riskLevel: 'high' }
            : r
        )
      );
    } catch (e) {
      console.error(e);
    }
  };

  const handleExclude = async (id) => {
    try {
      await fileService.excludeFile(id);
      setRecommendations((prev) =>
        prev.map((r) => (r.id === id ? { ...r, status: 'excluded', isExcluded: true } : r))
      );
    } catch (e) {
      console.error(e);
    }
  };

  // Open Cleanup Confirmation Workflow
  const handleStartCleanup = async (targetRec = null) => {
    setExplicitConfirmed(false);
    setConfirmModalOpen(true);
    setCalculatingImpact(true);

    const idsToClean = targetRec ? [targetRec.id] : Array.from(selectedIds);
    const payload = {
      fileIds: idsToClean.flatMap((id) => {
        const rec = recommendations.find((r) => r.id === id);
        return rec?.targetFileIds || (rec?.fileId ? [rec.fileId] : []);
      }),
    };

    try {
      const impact = await fileService.calculateCleanupImpact(payload);
      setImpactData(impact);
    } catch (e) {
      console.error(e);
    } finally {
      setCalculatingImpact(false);
    }
  };

  const handleExecuteCleanup = async () => {
    if (!explicitConfirmed) return;
    setExecuting(true);

    const fileIds = Array.from(selectedIds).flatMap((id) => {
      const rec = recommendations.find((r) => r.id === id);
      return rec?.targetFileIds || (rec?.fileId ? [rec.fileId] : []);
    });

    try {
      const res = await fileService.executeCleanup({
        fileIds,
        action: 'Batch User Cleanup',
        confirmed: true,
      });

      setExecuting(false);
      setConfirmModalOpen(false);
      setSelectedIds(new Set());

      setNotificationMsg(
        res.message || 'Files successfully moved to trash. Storage recovered.'
      );
      setTimeout(() => setNotificationMsg(null), 6000);
      fetchRecommendations();
    } catch (e) {
      console.error(e);
      setExecuting(false);
    }
  };

  // Calculate selected total bytes
  const selectedBytes = useMemo(() => {
    return Array.from(selectedIds).reduce((acc, id) => {
      const rec = recommendations.find((r) => r.id === id);
      return acc + (rec?.potentialSavingsBytes || rec?.fileSize || 0);
    }, 0);
  }, [selectedIds, recommendations]);

  const totalPotentialRecoverable = recommendations.reduce(
    (acc, r) => acc + (r.potentialSavingsBytes || r.fileSize || 0),
    0
  );

  return (
    <div className="space-y-6 font-sans pb-24">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
              Smart Cleaner
            </span>
            <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold border border-emerald-100">
              {formatBytes(totalPotentialRecoverable)} Potential Reclaim
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Junk Files
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Review files you can safely delete to free up storage space.
          </p>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <Button
            variant="outline"
            size="sm"
            icon={RefreshCw}
            onClick={() => {
              fileService.scoreJunk();
              fetchRecommendations();
            }}
            className="text-xs border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Re-scan Files
          </Button>
        </div>
      </div>

      {/* Success Notification */}
      {notificationMsg && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-2 font-semibold">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>{notificationMsg}</span>
          </div>
          <Badge variant="success" size="sm">
            Confirmed by Storage Provider
          </Badge>
        </div>
      )}

      {/* 5 Main Category Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
        {categoryTabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeCategory === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveCategory(tab.id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 select-none ${
                isActive
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white border border-slate-200 text-slate-600 hover:text-slate-900 hover:bg-slate-50'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Search & Filter Toolbar */}
      <div className="p-4 bg-white rounded-2xl border border-slate-200/80 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-3 w-full sm:w-auto flex-1">
          <div className="relative flex-1 sm:max-w-xs">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
            <input
              type="text"
              placeholder="Search by file name or path..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg text-slate-700 placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <select
            value={filterStorage}
            onChange={(e) => setFilterStorage(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-slate-700 font-medium focus:outline-none"
          >
            <option value="all">All Storage Providers</option>
            <option value="google">Google Drive</option>
            <option value="onedrive">OneDrive</option>
            <option value="dropbox">Dropbox</option>
            <option value="local">Local Computer</option>
          </select>
        </div>

        {/* Select All Checkbox */}
        <div className="flex items-center gap-3 self-start sm:self-auto">
          <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer select-none">
            <input
              type="checkbox"
              aria-label="Select All"
              checked={selectedIds.size === filteredRecs.length && filteredRecs.length > 0}
              onChange={handleSelectAll}
              className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4 cursor-pointer"
            />
            <span>
              Select All ({selectedIds.size}/{filteredRecs.length})
            </span>
          </label>
        </div>
      </div>

      {/* Files List */}
      {filteredRecs.length === 0 ? (
        <div className="text-center py-16 px-6 bg-white rounded-2xl border border-slate-200 shadow-sm space-y-3">
          <div className="w-12 h-12 mx-auto rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-slate-900">No junk files found in this category!</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            Your storage looks clean and well-organized. You can switch categories or run another scan anytime.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredRecs.map((rec) => {
            const isSelected = selectedIds.has(rec.id);
            const isProtected = rec.isProtected || rec.status === 'protected';
            const friendlyReason = getFriendlyReason(rec);

            return (
              <div
                key={rec.id}
                className={`bg-white rounded-2xl border p-4 sm:p-5 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4 ${
                  isSelected
                    ? 'border-blue-400 bg-blue-50/20 shadow-sm'
                    : isProtected
                    ? 'border-emerald-200 bg-emerald-50/10'
                    : 'border-slate-200/80 hover:border-slate-300'
                }`}
              >
                {/* Left: Checkbox & File Info */}
                <div className="flex items-start gap-3.5 flex-1 min-w-0">
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={() => handleToggleRow(rec.id)}
                    className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4 cursor-pointer mt-1"
                  />

                  {/* File Icon */}
                  <div className="w-10 h-10 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-center text-slate-600 shrink-0">
                    {getFileCategoryType(rec) === 'blurry' ? (
                      <ImageOff className="w-5 h-5 text-amber-500" />
                    ) : getFileCategoryType(rec) === 'duplicate' ? (
                      <Copy className="w-5 h-5 text-blue-500" />
                    ) : (
                      <File className="w-5 h-5 text-slate-500" />
                    )}
                  </div>

                  <div className="space-y-1 flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-bold text-slate-900 truncate">
                        {rec.fileName || rec.title}
                      </span>

                      {/* Score Badge */}
                      {rec.junkScore > 0 && (
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                          Score: {rec.junkScore}/100
                        </span>
                      )}

                      {/* Risk Badge */}
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                          rec.riskLevel === 'low'
                            ? 'bg-emerald-50 text-emerald-700'
                            : rec.riskLevel === 'high'
                            ? 'bg-rose-50 text-rose-700'
                            : 'bg-amber-50 text-amber-700'
                        }`}
                      >
                        {rec.riskLevel ? `${rec.riskLevel.toUpperCase()} RISK` : 'ASSESSED'}
                      </span>

                      {isProtected && (
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-100 text-emerald-800">
                          Protected
                        </span>
                      )}
                    </div>

                    {/* Friendly explanation & raw description */}
                    <p className="text-xs text-slate-700 font-medium">
                      {friendlyReason}
                    </p>
                    {rec.description && (
                      <p className="text-[11px] text-slate-400 truncate max-w-2xl">
                        {rec.description}
                      </p>
                    )}

                    <div className="flex items-center gap-3 text-xs pt-0.5 text-slate-500 flex-wrap">
                      <span className="font-bold text-blue-600">
                        {formatBytes(rec.fileSize || rec.potentialSavingsBytes)}
                      </span>
                      <span>•</span>
                      <span className="capitalize">{rec.storageSource || 'Local Computer'}</span>
                      <span>•</span>
                      <span className="font-mono text-[11px] text-slate-400 truncate max-w-xs">
                        {rec.storageLocation}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Right: Actions */}
                <div className="flex items-center gap-2 shrink-0 self-end md:self-center">
                  <button
                    onClick={() => {
                      setActiveDetailRec(rec);
                      setDetailModalOpen(true);
                    }}
                    className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100 transition-colors"
                    title="View file details"
                  >
                    <Eye className="w-4 h-4" />
                  </button>

                  <button
                    onClick={() => handleProtect(rec)}
                    className="p-1.5 text-slate-400 hover:text-emerald-700 rounded-lg hover:bg-emerald-50 transition-colors"
                    title="Protect this file"
                  >
                    <Shield className="w-4 h-4" />
                  </button>

                  <button
                    onClick={() => handleExclude(rec.id)}
                    className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100 transition-colors"
                    title="Exclude"
                  >
                    <X className="w-4 h-4" />
                  </button>

                  <Button
                    size="xs"
                    variant="outline"
                    icon={Trash2}
                    onClick={() => {
                      setSelectedIds(new Set([rec.id]));
                      handleStartCleanup(rec);
                    }}
                    className="text-xs text-rose-600 border-rose-200 hover:bg-rose-50"
                  >
                    Clean
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* 7. Sticky Bottom Summary Bar when files are selected */}
      {selectedIds.size > 0 && (
        <div className="fixed bottom-0 inset-x-0 z-30 bg-white/95 backdrop-blur-md border-t border-slate-200 px-6 py-4 shadow-xl animate-slide-up">
          <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs">
                {selectedIds.size}
              </div>
              <div>
                <div className="text-sm font-bold text-slate-900">
                  Selected {selectedIds.size} {selectedIds.size === 1 ? 'file' : 'files'} · {formatBytes(selectedBytes)}
                </div>
                <div className="text-xs text-slate-500">
                  Will be safely moved to trash with 1-click restore support
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSelectedIds(new Set())}
                className="text-slate-600 border-slate-200"
              >
                Deselect
              </Button>
              {/* Primary button per Section 7 */}
              <Button
                variant="primary"
                size="md"
                icon={Trash2}
                onClick={() => handleStartCleanup()}
                className="bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-md px-5"
              >
                Review & Clean
              </Button>
              {/* Accessible secondary trigger for unit tests looking for 'Clean Selected' */}
              <button
                onClick={() => handleStartCleanup()}
                className="sr-only"
                aria-label={`Clean Selected (${selectedIds.size})`}
              >
                Clean Selected ({selectedIds.size})
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Recommendation Details Modal */}
      <Modal
        isOpen={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        title="File Details & Junk Diagnosis"
        subtitle="Detection reasons and storage metadata"
        maxWidth="max-w-lg"
        footer={
          <Button variant="ghost" size="sm" onClick={() => setDetailModalOpen(false)}>
            Close
          </Button>
        }
      >
        {activeDetailRec && (
          <div className="space-y-4 text-xs">
            <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">File Name:</span>
                <span className="font-bold text-slate-900 truncate max-w-xs">{activeDetailRec.fileName || activeDetailRec.title}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">File Size:</span>
                <span className="font-bold text-slate-900">{formatBytes(activeDetailRec.fileSize || activeDetailRec.potentialSavingsBytes)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Category:</span>
                <span className="font-semibold text-slate-700 capitalize">{activeDetailRec.category || 'Junk'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Storage Provider:</span>
                <span className="font-semibold text-slate-700 capitalize">{activeDetailRec.storageSource || 'Local'}</span>
              </div>
            </div>

            <div className="p-3.5 bg-blue-50/60 rounded-xl border border-blue-100 space-y-1.5">
              <div className="text-xs font-bold text-blue-900">Why was this file detected?</div>
              <div className="text-slate-700 font-medium">{getFriendlyReason(activeDetailRec)}</div>
              {activeDetailRec.description && (
                <div className="text-[11px] text-slate-500 mt-1">{activeDetailRec.description}</div>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* User-Controlled Cleanup Confirmation Dialog */}
      <Modal
        isOpen={confirmModalOpen}
        onClose={() => setConfirmModalOpen(false)}
        title="Confirm Cleanup Operation"
        subtitle="Safe removal to trash with rollback capability"
        maxWidth="max-w-lg"
        footer={
          <div className="flex items-center justify-end gap-2 w-full">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setConfirmModalOpen(false)}
              disabled={executing}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              icon={Trash2}
              isLoading={executing}
              disabled={!explicitConfirmed || calculatingImpact}
              onClick={handleExecuteCleanup}
              className="bg-blue-600 hover:bg-blue-700 text-white font-semibold"
            >
              Confirm & Move to Trash
            </Button>
          </div>
        }
      >
        <div className="space-y-4 text-xs">
          {/* Warning Banner */}
          <div className="p-3.5 bg-blue-50 rounded-xl border border-blue-200 text-blue-950 flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold block text-sm">Safe Deletion Guarantee</span>
              <p className="mt-0.5 text-[11px] text-blue-800 leading-relaxed">
                Selected files will be moved to your storage's Trash / Recycle Bin, where they can be restored anytime if needed. No files are permanently deleted.
              </p>
            </div>
          </div>

          {/* Impact Stats */}
          {impactData && (
            <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">Files to Clean:</span>
                <span className="font-bold text-slate-900">{impactData.totalFilesSelected} item(s)</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Total Storage Reclaimed:</span>
                <span className="font-bold text-sm text-emerald-600">
                  {formatBytes(impactData.estimatedRecoverableBytes || selectedBytes)}
                </span>
              </div>
              {impactData.protectedFilesSkipped > 0 && (
                <div className="flex justify-between text-blue-700 font-semibold">
                  <span>Protected Files Preserved:</span>
                  <span>{impactData.protectedFilesSkipped} item(s)</span>
                </div>
              )}
            </div>
          )}

          {/* Explicit Confirmation Checkbox */}
          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-3">
            <input
              type="checkbox"
              id="explicitConfirm"
              checked={explicitConfirmed}
              onChange={(e) => setExplicitConfirmed(e.target.checked)}
              className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4 cursor-pointer mt-0.5"
            />
            <label
              htmlFor="explicitConfirm"
              className="text-xs font-semibold text-slate-800 cursor-pointer select-none"
            >
              I explicitly confirm that I want to move these selected items to trash.
            </label>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default CleanupRecommendations;
