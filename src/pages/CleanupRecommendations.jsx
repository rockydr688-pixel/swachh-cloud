import React, { useState, useEffect } from 'react';
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
  Archive,
  ArrowRight,
  HardDrive,
  Info,
  Check,
  X,
  Eye,
  Shield,
  Filter,
  RefreshCw,
  SlidersHorizontal,
} from 'lucide-react';

export const CleanupRecommendations = () => {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [filterCategory, setFilterCategory] = useState('all');
  const [filterRisk, setFilterRisk] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');

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
      const res = await fileService.getRecommendations({
        category: filterCategory !== 'all' ? filterCategory : undefined,
        riskLevel: filterRisk !== 'all' ? filterRisk : undefined,
        status: filterStatus !== 'all' ? filterStatus : undefined,
      });
      setRecommendations(res.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecommendations();
  }, [filterCategory, filterRisk, filterStatus]);

  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedIds(new Set(recommendations.map((r) => r.id)));
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

  const handleAccept = async (id) => {
    await fileService.acceptRecommendation(id);
    setRecommendations((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status: 'accepted' } : r))
    );
  };

  const handleReject = async (id) => {
    await fileService.rejectRecommendation(id);
    setRecommendations((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status: 'rejected' } : r))
    );
  };

  const handleExclude = async (id) => {
    await fileService.excludeFile(id);
    setRecommendations((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status: 'excluded', isExcluded: true } : r))
    );
  };

  const handleProtect = async (rec) => {
    const fileId = rec.fileId || (rec.targetFileIds && rec.targetFileIds[0]);
    if (!fileId) return;
    await fileService.protectFile(fileId);
    setRecommendations((prev) =>
      prev.map((r) =>
        r.id === rec.id
          ? { ...r, status: 'protected', isProtected: true, recommendedAction: 'Keep Original', riskLevel: 'high' }
          : r
      )
    );
  };

  // Open Cleanup Confirmation Workflow
  const handleStartCleanup = async (targetRec = null) => {
    setExplicitConfirmed(false);
    setConfirmModalOpen(true);
    setCalculatingImpact(true);

    const payload = targetRec
      ? { recommendationId: targetRec.id }
      : {
          fileIds: Array.from(selectedIds).flatMap((id) => {
            const rec = recommendations.find((r) => r.id === id);
            return rec?.targetFileIds || (rec?.fileId ? [rec.fileId] : []);
          }),
        };

    const impact = await fileService.calculateCleanupImpact(payload);
    setImpactData(impact);
    setCalculatingImpact(false);
  };

  const handleExecuteCleanup = async () => {
    if (!explicitConfirmed) return;
    setExecuting(true);

    const targetRecId = selectedIds.size === 1 ? Array.from(selectedIds)[0] : null;
    const fileIds = Array.from(selectedIds).flatMap((id) => {
      const rec = recommendations.find((r) => r.id === id);
      return rec?.targetFileIds || (rec?.fileId ? [rec.fileId] : []);
    });

    const res = await fileService.executeCleanup({
      recommendationId: targetRecId,
      fileIds,
      action: 'Batch User Cleanup',
      confirmed: true,
    });

    setExecuting(false);
    setConfirmModalOpen(false);
    setSelectedIds(new Set());

    setNotificationMsg(
      res.message || 'Cleanup operation executed successfully. Files safely relocated to trash.'
    );
    setTimeout(() => setNotificationMsg(null), 6000);
    fetchRecommendations();
  };

  const totalRecoverable = recommendations.reduce(
    (acc, r) => acc + (r.potentialSavingsBytes || r.fileSize || 0),
    0
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Smart Cleanup Center & Recommendations"
        subtitle="User-controlled storage optimization with multi-signal junk scoring, file protection whitelisting, and provider-confirmed trash routing."
        breadcrumbs={[{ label: 'Cleanup Recommendations' }]}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              icon={RefreshCw}
              onClick={() => {
                fileService.scoreJunk();
                fetchRecommendations();
              }}
            >
              Re-Scan & Optimize
            </Button>
            <Badge variant="indigo" size="md">
              {recommendations.length} Active Items
            </Badge>
          </div>
        }
      />

      {/* Success Notification */}
      {notificationMsg && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 flex items-center justify-between">
          <div className="flex items-center gap-2 font-semibold">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>{notificationMsg}</span>
          </div>
          <Badge variant="success" size="sm">
            Confirmed by Provider
          </Badge>
        </div>
      )}

      {/* Reclaim Highlight Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-indigo-900 via-slate-900 to-[#0B132B] text-white border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-card">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-indigo-400" />
            <h3 className="text-lg font-bold text-white">
              Estimated Total Reclaim: {formatBytes(totalRecoverable)}
            </h3>
          </div>
          <p className="text-xs text-slate-300 max-w-xl">
            Clean byte clones, motion-blurred burst shots, and stale large archives without risking protected files. Zero storage is claimed until confirmed by the storage daemon.
          </p>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <Button
            variant="primary"
            size="md"
            icon={Trash2}
            className="bg-indigo-600 hover:bg-indigo-500 font-bold"
            disabled={selectedIds.size === 0 && recommendations.length === 0}
            onClick={() => handleStartCleanup()}
          >
            {selectedIds.size > 0
              ? `Clean Selected (${selectedIds.size})`
              : 'Clean All High-Risk Items'}
          </Button>
        </div>
      </div>

      {/* AI Scoring Architecture & Safeguards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="p-4 border-slate-200 bg-slate-50/70">
          <div className="flex items-center gap-2 mb-1.5">
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
            <span className="text-xs font-bold text-slate-800 uppercase tracking-wide">
              Additive Scoring Model
            </span>
          </div>
          <p className="text-xs text-slate-600">
            Junk scores scale 0–100 across 5 independent signals: Duplicate (+30), Temp (+25), Blur (+20), Inactive (+15), Large (+10).
          </p>
        </Card>

        <Card className="p-4 border-slate-200 bg-slate-50/70">
          <div className="flex items-center gap-2 mb-1.5">
            <AlertTriangle className="w-4 h-4 text-amber-600" />
            <span className="text-xs font-bold text-slate-800 uppercase tracking-wide">
              User-Controlled Execution
            </span>
          </div>
          <p className="text-xs text-slate-600">
            No files are permanently or blindly deleted. Deletions require explicit modal confirmation and route to staging recycle bins.
          </p>
        </Card>

        <Card className="p-4 border-slate-200 bg-slate-50/70">
          <div className="flex items-center gap-2 mb-1.5">
            <Shield className="w-4 h-4 text-indigo-600" />
            <span className="text-xs font-bold text-slate-800 uppercase tracking-wide">
              File Protection Whitelist
            </span>
          </div>
          <p className="text-xs text-slate-600">
            Mark important files as protected to permanently shield them from junk scores, automated flags, and batch purges.
          </p>
        </Card>
      </div>

      {/* Filter and Selection Controls Toolbar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              aria-label="Select All"
              checked={selectedIds.size === recommendations.length && recommendations.length > 0}
              onChange={handleSelectAll}
              className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4 cursor-pointer"
            />
            <span className="text-xs font-semibold text-slate-700">
              Select All ({selectedIds.size}/{recommendations.length})
            </span>
          </div>

          <div className="h-4 w-px bg-slate-200 hidden sm:block" />

          {/* Category Filter */}
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-700 font-medium"
          >
            <option value="all">All Categories</option>
            <option value="Duplicates">Duplicates</option>
            <option value="Large Files">Large Files</option>
            <option value="Blurry Images">Blurry Images</option>
            <option value="Inactive Files">Inactive Files</option>
          </select>

          {/* Risk Level Filter */}
          <select
            value={filterRisk}
            onChange={(e) => setFilterRisk(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-700 font-medium"
          >
            <option value="all">All Risk Levels</option>
            <option value="low">Low Risk (Safe)</option>
            <option value="medium">Medium Risk (Review)</option>
            <option value="high">High Risk (Protected)</option>
          </select>

          {/* Status Filter */}
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-700 font-medium"
          >
            <option value="all">All Statuses</option>
            <option value="pending">Pending</option>
            <option value="accepted">Accepted</option>
            <option value="rejected">Rejected</option>
            <option value="excluded">Excluded</option>
            <option value="protected">Protected</option>
          </select>
        </div>

        {selectedIds.size > 0 && (
          <Button
            variant="danger"
            size="sm"
            icon={Trash2}
            onClick={() => handleStartCleanup()}
          >
            Clean ({selectedIds.size}) Selected
          </Button>
        )}
      </div>

      {/* Recommendations List / Table */}
      <div className="space-y-4">
        {recommendations.map((rec) => {
          const isSelected = selectedIds.has(rec.id);
          const isProtected = rec.isProtected || rec.status === 'protected';
          const isExcluded = rec.isExcluded || rec.status === 'excluded';

          return (
            <Card
              key={rec.id}
              className={`hover:border-slate-300 transition-all p-5 ${
                isSelected ? 'bg-indigo-50/40 border-indigo-200' : ''
              } ${isProtected ? 'bg-emerald-50/30 border-emerald-200' : ''}`}
            >
              <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
                <div className="flex items-start gap-3 flex-1 min-w-0">
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={() => handleToggleRow(rec.id)}
                    className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4 cursor-pointer mt-1"
                  />

                  <div className="space-y-2 flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-bold text-slate-900 truncate">
                        {rec.fileName || rec.title}
                      </span>

                      <Badge
                        variant={
                          rec.riskLevel === 'low'
                            ? 'success'
                            : rec.riskLevel === 'high'
                            ? 'danger'
                            : 'warning'
                        }
                        size="xs"
                        dot
                      >
                        {rec.riskLevel ? `${rec.riskLevel.toUpperCase()} RISK` : 'ASSESSED'}
                      </Badge>

                      <Badge variant="navy" size="xs">
                        {rec.category}
                      </Badge>

                      {rec.junkScore > 0 && (
                        <span className="text-[11px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                          Score: {rec.junkScore}/100
                        </span>
                      )}

                      {isProtected && (
                        <Badge variant="success" size="xs">
                          Protected
                        </Badge>
                      )}

                      {isExcluded && (
                        <Badge variant="neutral" size="xs">
                          Excluded
                        </Badge>
                      )}
                    </div>

                    <p className="text-xs text-slate-600 leading-relaxed max-w-3xl">
                      {rec.description}
                    </p>

                    <div className="flex items-center gap-4 text-xs pt-1 flex-wrap">
                      <span className="text-indigo-600 font-bold">
                        Reclaim: {formatBytes(rec.fileSize || rec.potentialSavingsBytes)}
                      </span>
                      <span className="text-slate-400">•</span>
                      <span className="text-slate-600 font-medium capitalize">
                        Source: {rec.storageSource || 'Local Daemon'}
                      </span>
                      <span className="text-slate-400">•</span>
                      <span className="text-slate-500 font-mono text-[11px] truncate max-w-xs">
                        {rec.storageLocation}
                      </span>
                    </div>

                    <div className="p-2 rounded-lg bg-slate-50 border border-slate-100 text-[11px] text-slate-700 font-medium flex items-center gap-2">
                      <span className="text-indigo-600 font-bold">Recommended Action:</span>
                      <span>{rec.recommendedAction || rec.suggestedAction}</span>
                    </div>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center gap-1.5 shrink-0 pt-2 md:pt-0">
                  <Button
                    size="xs"
                    variant="ghost"
                    icon={Eye}
                    title="Inspect details"
                    onClick={() => {
                      setActiveDetailRec(rec);
                      setDetailModalOpen(true);
                    }}
                  >
                    Details
                  </Button>

                  <Button
                    size="xs"
                    variant="outline"
                    icon={Shield}
                    title="Whitelist & Protect file"
                    onClick={() => handleProtect(rec)}
                  >
                    Protect
                  </Button>

                  <Button
                    size="xs"
                    variant="outline"
                    icon={X}
                    title="Exclude"
                    onClick={() => handleExclude(rec.id)}
                  >
                    Exclude
                  </Button>

                  <Button
                    size="xs"
                    variant="danger"
                    icon={Trash2}
                    onClick={() => {
                      setSelectedIds(new Set([rec.id]));
                      handleStartCleanup(rec);
                    }}
                  >
                    Clean
                  </Button>
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Recommendation Details Modal */}
      <Modal
        isOpen={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        title="Recommendation Intelligence & Diagnostics"
        subtitle="Detailed detection criteria and risk metadata"
        maxWidth="max-w-xl"
        footer={
          <Button variant="ghost" size="sm" onClick={() => setDetailModalOpen(false)}>
            Close
          </Button>
        }
      >
        {activeDetailRec && (
          <div className="space-y-4 text-xs">
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-1.5">
              <div className="flex justify-between">
                <span className="text-slate-500">File Name:</span>
                <span className="font-bold text-slate-900">{activeDetailRec.fileName || activeDetailRec.title}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">File Size:</span>
                <span className="font-bold text-slate-900">{formatBytes(activeDetailRec.fileSize || activeDetailRec.potentialSavingsBytes)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">File Type:</span>
                <span className="font-semibold text-slate-700 capitalize">{activeDetailRec.fileType || 'other'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Storage Source:</span>
                <span className="font-semibold text-slate-700 capitalize">{activeDetailRec.storageSource || 'local'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Analysis Date:</span>
                <span className="text-slate-700">{formatDate(activeDetailRec.analysisDate || activeDetailRec.createdAt)}</span>
              </div>
            </div>

            <div className="p-3 bg-indigo-50/60 rounded-xl border border-indigo-100 space-y-1">
              <span className="text-indigo-900 font-bold uppercase block text-[11px]">
                Junk Score Assessment
              </span>
              <div className="flex items-center justify-between">
                <span>Composite Score:</span>
                <span className="font-extrabold text-sm text-indigo-700">{activeDetailRec.junkScore || 30}/100</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Risk Level:</span>
                <Badge variant={activeDetailRec.riskLevel === 'low' ? 'success' : 'warning'} size="xs">
                  {activeDetailRec.riskLevel || 'medium'}
                </Badge>
              </div>
              <div className="flex items-center justify-between">
                <span>Recommended Action:</span>
                <span className="font-bold text-slate-800">{activeDetailRec.recommendedAction || 'Review & Delete'}</span>
              </div>
            </div>

            {activeDetailRec.detectionReasons && activeDetailRec.detectionReasons.length > 0 && (
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                <span className="font-bold text-slate-800 block mb-1.5 uppercase text-[11px]">
                  Detection Evidence & Reasons:
                </span>
                <ul className="list-disc list-inside space-y-1 text-slate-600">
                  {activeDetailRec.detectionReasons.map((reason, idx) => (
                    <li key={idx}>{reason}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* User-Controlled Cleanup Confirmation Dialog */}
      <Modal
        isOpen={confirmModalOpen}
        onClose={() => setConfirmModalOpen(false)}
        title="Confirm Cleanup Operation"
        subtitle="Explicit verification required before moving files to trash"
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
            >
              Confirm & Move to Trash
            </Button>
          </div>
        }
      >
        <div className="space-y-4 text-xs">
          {/* Warning Banner */}
          <div className="p-3.5 bg-amber-50 rounded-xl border border-amber-200 text-amber-900 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold block text-sm">Storage Deletion Warning</span>
              <p className="mt-0.5 text-[11px] leading-relaxed">
                Files will be safely relocated to your local OS Recycle Bin or Google Drive Trash. <strong>Storage will only be marked recovered once the storage daemon confirms the operation.</strong> You can restore these files anytime from the Cleanup History page.
              </p>
            </div>
          </div>

          {/* Impact Stats */}
          {impactData && (
            <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">Items to Clean:</span>
                <span className="font-bold text-slate-900">{impactData.totalFilesSelected} item(s)</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Estimated Reclaim:</span>
                <span className="font-extrabold text-sm text-emerald-600">
                  {formatBytes(impactData.estimatedRecoverableBytes)}
                </span>
              </div>
              {impactData.protectedFilesSkipped > 0 && (
                <div className="flex justify-between text-indigo-700 font-semibold">
                  <span>Protected Files Preserved:</span>
                  <span>{impactData.protectedFilesSkipped} item(s)</span>
                </div>
              )}
            </div>
          )}

          {/* Explicit Confirmation Checkbox */}
          <div className="p-3.5 rounded-xl bg-slate-100/80 border border-slate-300 flex items-start gap-3">
            <input
              type="checkbox"
              id="explicitConfirm"
              checked={explicitConfirmed}
              onChange={(e) => setExplicitConfirmed(e.target.checked)}
              className="rounded text-rose-600 focus:ring-rose-500 h-4 w-4 cursor-pointer mt-0.5"
            />
            <label
              htmlFor="explicitConfirm"
              className="text-xs font-semibold text-slate-900 cursor-pointer select-none"
            >
              I explicitly confirm that I want to move these selected items to trash. I understand this action alters disk storage.
            </label>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default CleanupRecommendations;
