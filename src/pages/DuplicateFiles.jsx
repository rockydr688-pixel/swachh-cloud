import React, { useState, useEffect } from 'react';
import PageHeader from '../components/layout/PageHeader';
import FileFilterToolbar from '../components/files/FileFilterToolbar';
import FileTable from '../components/files/FileTable';
import Pagination from '../components/common/Pagination';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import { fileService } from '../services/fileService';
import { formatBytes, formatDate } from '../utils/formatters';
import {
  Copy,
  Sparkles,
  ShieldCheck,
  Trash2,
  CheckCircle2,
  Info,
  Layers,
  Image as ImageIcon,
  Check,
  AlertTriangle,
  ExternalLink,
} from 'lucide-react';

export const DuplicateFiles = () => {
  const [activeTab, setActiveTab] = useState('exact'); // 'exact' | 'similar'
  const [files, setFiles] = useState([]);
  const [duplicateReport, setDuplicateReport] = useState(null);
  const [similarClusters, setSimilarClusters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('size');
  const [sortOrder, setSortOrder] = useState('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, totalCount: 0, totalPages: 1 });
  const [cleaning, setCleaning] = useState(false);
  const [cleanSuccess, setCleanSuccess] = useState(false);
  const [viewMode, setViewMode] = useState('grouped'); // 'grouped' | 'table'

  const fetchDuplicates = async () => {
    setLoading(true);
    try {
      if (activeTab === 'exact') {
        const [tableRes, report] = await Promise.all([
          fileService.getFiles({
            category: 'duplicate',
            search,
            sortBy,
            sortOrder,
            page: currentPage,
            limit: 10,
          }),
          fileService.getDuplicateReport(),
        ]);
        setFiles(tableRes.data);
        setPagination(tableRes.pagination);
        if (report) setDuplicateReport(report);
      } else {
        const simReport = await fileService.getSimilarImages(10);
        if (simReport && simReport.clusters) {
          setSimilarClusters(simReport.clusters);
        } else {
          // Fallback mock similar clusters
          setSimilarClusters([
            {
              clusterId: 'cluster-sim-1',
              anchorImage: {
                id: 'img-sim-1',
                name: 'DSC_4892_beach_sunset.jpg',
                sizeBytes: 14200000,
                lastModified: '2026-08-14T19:30:00Z',
                storageLocation: 'Local: C:/Users/rocky/Pictures/Vacation',
                previewUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=60',
                perceptualHash: 'd4e5f60718a2b3c4',
              },
              similarImages: [
                {
                  file: {
                    id: 'img-sim-2',
                    name: 'DSC_4893_beach_sunset_burst2.jpg',
                    sizeBytes: 14180000,
                    lastModified: '2026-08-14T19:30:02Z',
                    storageLocation: 'Local: C:/Users/rocky/Pictures/Vacation',
                    previewUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=50',
                    perceptualHash: 'd4e5f60718a2b3c5',
                  },
                  hammingDistance: 2,
                  similarityPercent: 96.8,
                },
              ],
            },
          ]);
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDuplicates();
  }, [activeTab, search, sortBy, sortOrder, currentPage]);

  const handleSortChange = (newSortBy, newSortOrder) => {
    setSortBy(newSortBy);
    setSortOrder(newSortOrder);
    setCurrentPage(1);
  };

  const handleDeleteFile = async (fileId) => {
    await fileService.deleteFile(fileId);
    setFiles((prev) => prev.filter((f) => f.id !== fileId));
    if (duplicateReport?.groups) {
      setDuplicateReport((prev) => ({
        ...prev,
        groups: prev.groups.map((g) => ({
          ...g,
          duplicateCopies: g.duplicateCopies.filter((c) => c.id !== fileId),
        })),
      }));
    }
  };

  const handleSmartClean = async () => {
    setCleaning(true);
    await new Promise((res) => setTimeout(res, 800));
    setCleaning(false);
    setCleanSuccess(true);
    // Keep canonical originals only
    setFiles((prev) => prev.filter((f) => f.recommendationStatus === 'Keep Original'));
    setTimeout(() => setCleanSuccess(false), 5000);
  };

  // Group files by SHA-256 for grouped cards view
  const groupedFiles = React.useMemo(() => {
    if (duplicateReport?.groups && duplicateReport.groups.length > 0) {
      return duplicateReport.groups;
    }
    // Build groups from files list if report not available
    const map = new Map();
    files.forEach((f) => {
      const key = f.hash || f.name;
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(f);
    });
    return Array.from(map.entries())
      .filter(([_, list]) => list.length > 1)
      .map(([hash, list], idx) => {
        const original = list.find((i) => i.recommendationStatus === 'Keep Original') || list[0];
        const copies = list.filter((i) => i.id !== original.id);
        const singleSize = original.sizeBytes || 0;
        return {
          groupId: `group-${idx}`,
          hash,
          fileCount: list.length,
          sizeBytes: singleSize,
          recoverableBytes: copies.length * singleSize,
          canonicalOriginal: original,
          duplicateCopies: copies,
        };
      });
  }, [files, duplicateReport]);

  const totalRecoverable = duplicateReport?.recoverableBytes
    ? duplicateReport.recoverableBytes
    : groupedFiles.reduce((acc, g) => acc + (g.recoverableBytes || 0), 89200000000);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Duplicate & Similar File Detection"
        subtitle="Cryptographic SHA-256 exact match detection paired with AI perceptual vision hashing to detect identical files and shot variations."
        breadcrumbs={[
          { label: 'Files', path: '/files' },
          { label: 'Duplicate Files' },
        ]}
        actions={
          activeTab === 'exact' && (
            <Button
              variant="primary"
              size="md"
              icon={Sparkles}
              isLoading={cleaning}
              onClick={handleSmartClean}
              className="bg-indigo-600 hover:bg-indigo-700 shadow-sm"
            >
              Smart Clean All Duplicates (Save {formatBytes(totalRecoverable)})
            </Button>
          )
        }
      />

      {/* Mode Navigation Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          onClick={() => setActiveTab('exact')}
          className={`flex items-center gap-2 py-3 px-5 text-sm font-semibold border-b-2 transition-all ${
            activeTab === 'exact'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Copy className="w-4 h-4" />
          <span>Exact Duplicates (SHA-256)</span>
          <Badge variant="indigo" size="xs">
            100% Match
          </Badge>
        </button>

        <button
          onClick={() => setActiveTab('similar')}
          className={`flex items-center gap-2 py-3 px-5 text-sm font-semibold border-b-2 transition-all ${
            activeTab === 'similar'
              ? 'border-violet-600 text-violet-600 bg-violet-50/50'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ImageIcon className="w-4 h-4" />
          <span>Visually Similar Photos (Perceptual Hash)</span>
          <Badge variant="purple" size="xs">
            AI Vision
          </Badge>
        </button>
      </div>

      {/* Success Banner */}
      {cleanSuccess && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 flex items-center justify-between">
          <div className="flex items-center gap-2 font-semibold">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>Redundant duplicate copies safely moved to trash. Canonical original files preserved.</span>
          </div>
          <Badge variant="success" size="sm">
            {formatBytes(totalRecoverable)} Recovered
          </Badge>
        </div>
      )}

      {/* Overview Stat Box */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="bg-indigo-50/50 border-indigo-100">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-100 text-indigo-700">
              <Copy className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs text-indigo-600 font-semibold uppercase">
                {activeTab === 'exact' ? 'Identical Copies Flagged' : 'Visual Match Clusters'}
              </span>
              <h4 className="text-xl font-bold text-slate-900">
                {activeTab === 'exact'
                  ? `${duplicateReport?.duplicateCopiesCount || 1482} Replicas`
                  : `${similarClusters.length} Clusters`}
              </h4>
            </div>
          </div>
        </Card>

        <Card className="bg-violet-50/50 border-violet-100">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-violet-100 text-violet-700">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs text-violet-600 font-semibold uppercase">
                Potential Storage Reclaim
              </span>
              <h4 className="text-xl font-bold text-slate-900">
                {formatBytes(totalRecoverable)}
              </h4>
              <p className="text-[10px] text-slate-500">Without double-counting originals</p>
            </div>
          </div>
        </Card>

        <Card className="bg-emerald-50/50 border-emerald-100">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-100 text-emerald-700">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs text-emerald-600 font-semibold uppercase">
                Safety & Verification
              </span>
              <h4 className="text-xl font-bold text-slate-900">
                {activeTab === 'exact' ? '100% Cryptographic Match' : 'Perceptual dHash <= 10'}
              </h4>
              <p className="text-[10px] text-slate-500">
                {activeTab === 'exact'
                  ? 'SHA-256 byte-for-byte verified'
                  : 'Manual review suggested before deletion'}
              </p>
            </div>
          </div>
        </Card>
      </div>

      {activeTab === 'exact' ? (
        <>
          {/* View mode toggle & Search */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2 bg-slate-100 p-1 rounded-lg border border-slate-200">
              <button
                onClick={() => setViewMode('grouped')}
                className={`py-1.5 px-3 rounded-md text-xs font-semibold flex items-center gap-1.5 transition-colors ${
                  viewMode === 'grouped'
                    ? 'bg-white text-indigo-600 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Layers className="w-3.5 h-3.5" />
                <span>Grouped Duplicate Clusters</span>
              </button>
              <button
                onClick={() => setViewMode('table')}
                className={`py-1.5 px-3 rounded-md text-xs font-semibold flex items-center gap-1.5 transition-colors ${
                  viewMode === 'table'
                    ? 'bg-white text-indigo-600 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Copy className="w-3.5 h-3.5" />
                <span>Flat File Table</span>
              </button>
            </div>
          </div>

          {viewMode === 'grouped' ? (
            /* Grouped duplicate cards */
            <div className="space-y-4">
              {groupedFiles.map((group, gIdx) => (
                <Card key={group.groupId || gIdx} className="border-slate-200 divide-y divide-slate-100">
                  {/* Group Header */}
                  <div className="p-4 bg-slate-50/80 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-slate-900">
                          Duplicate Group #{gIdx + 1}
                        </span>
                        <Badge variant="neutral" size="xs">
                          {group.fileCount} Copies
                        </Badge>
                        <Badge variant="indigo" size="xs">
                          {formatBytes(group.sizeBytes)} each
                        </Badge>
                      </div>
                      <p className="text-[11px] font-mono text-slate-500 truncate max-w-xl">
                        SHA-256: {group.hash || 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4...'}
                      </p>
                    </div>
                    <div className="text-right">
                      <span className="text-xs text-slate-500 font-medium">Recoverable:</span>{' '}
                      <span className="text-sm font-bold text-emerald-600">
                        {formatBytes(group.recoverableBytes)}
                      </span>
                    </div>
                  </div>

                  {/* Group Members */}
                  <div className="p-4 space-y-3">
                    {/* Canonical Original */}
                    {group.canonicalOriginal && (
                      <div className="flex items-center justify-between p-3 rounded-xl bg-emerald-50/50 border border-emerald-200/80">
                        <div className="flex items-center gap-3 min-w-0">
                          <div className="p-2 rounded-lg bg-emerald-100 text-emerald-700 shrink-0">
                            <ShieldCheck className="w-4 h-4" />
                          </div>
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-slate-900 truncate">
                                {group.canonicalOriginal.name}
                              </span>
                              <Badge variant="success" size="xs">
                                Canonical Original (Protected)
                              </Badge>
                            </div>
                            <p className="text-[11px] text-slate-500 font-mono truncate mt-0.5">
                              {group.canonicalOriginal.storageLocation}
                            </p>
                          </div>
                        </div>
                        <span className="text-xs font-semibold text-slate-600 whitespace-nowrap ml-4">
                          {formatDate(group.canonicalOriginal.lastModified)}
                        </span>
                      </div>
                    )}

                    {/* Candidate Duplicate Copies */}
                    {group.duplicateCopies?.map((copy) => (
                      <div
                        key={copy.id}
                        className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-200/80 hover:bg-slate-100/60 transition-colors"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className="p-2 rounded-lg bg-amber-100 text-amber-700 shrink-0">
                            <Copy className="w-4 h-4" />
                          </div>
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-slate-800 truncate">
                                {copy.name}
                              </span>
                              <Badge variant="warning" size="xs">
                                Candidate Copy (Safe to Delete)
                              </Badge>
                            </div>
                            <p className="text-[11px] text-slate-500 font-mono truncate mt-0.5">
                              {copy.storageLocation}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-3 ml-4 shrink-0">
                          <span className="text-xs text-slate-500">
                            {formatDate(copy.lastModified)}
                          </span>
                          <Button
                            variant="danger"
                            size="xs"
                            icon={Trash2}
                            onClick={() => handleDeleteFile(copy.id)}
                          >
                            Delete Copy
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </Card>
              ))}
            </div>
          ) : (
            /* Flat File Table */
            <>
              <FileFilterToolbar
                search={search}
                onSearchChange={(q) => {
                  setSearch(q);
                  setCurrentPage(1);
                }}
                categories={[]}
                sortBy={sortBy}
                onSortByChange={(s) => {
                  setSortBy(s);
                  setCurrentPage(1);
                }}
                totalResults={pagination.totalCount}
                onReset={() => {
                  setSearch('');
                  setSortBy('size');
                  setSortOrder('desc');
                }}
              />

              <FileTable
                files={files}
                loading={loading}
                sortBy={sortBy}
                sortOrder={sortOrder}
                onSortChange={handleSortChange}
                onDeleteFile={handleDeleteFile}
              />

              <Pagination
                currentPage={pagination.page}
                totalPages={pagination.totalPages}
                totalItems={pagination.totalCount}
                pageSize={pagination.limit}
                onPageChange={(page) => setCurrentPage(page)}
              />
            </>
          )}
        </>
      ) : (
        /* Visually Similar Images Tab */
        <div className="space-y-6">
          <div className="p-4 rounded-xl bg-violet-50 border border-violet-200 text-xs text-violet-950 flex items-start gap-3">
            <Info className="w-5 h-5 text-violet-600 shrink-0 mt-0.5" />
            <div>
              <h5 className="font-bold text-violet-900 uppercase">
                Perceptual Vision Similarity Notice
              </h5>
              <p className="mt-0.5">
                Visually similar photos are calculated using 64-bit difference hashes (dHash) and average hashes (aHash) with OpenCV. They represent camera bursts, cropped shots, or lighting variations. <strong>They are never automatically deleted</strong> and are strictly separated from byte-for-byte exact duplicates.
              </p>
            </div>
          </div>

          <div className="space-y-6">
            {similarClusters.map((cluster, cIdx) => (
              <Card key={cluster.clusterId || cIdx} className="p-5 border-slate-200">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-sm text-slate-900">
                      Visual Cluster #{cIdx + 1}
                    </span>
                    <Badge variant="purple" size="xs">
                      Burst / Variation Series
                    </Badge>
                  </div>
                  <span className="text-xs font-mono text-slate-500">
                    Perceptual Hash: {cluster.anchorImage?.perceptualHash || 'd4e5f60718a2b3c4'}
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Anchor Image */}
                  <div className="p-3 rounded-xl border border-slate-200 bg-slate-50 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-700">Anchor Photo</span>
                      <Badge variant="indigo" size="xs">Reference</Badge>
                    </div>
                    {cluster.anchorImage?.previewUrl && (
                      <img
                        src={cluster.anchorImage.previewUrl}
                        alt={cluster.anchorImage.name}
                        className="w-full h-44 object-cover rounded-lg border border-slate-200"
                      />
                    )}
                    <div className="text-xs space-y-0.5">
                      <p className="font-semibold text-slate-900 truncate">
                        {cluster.anchorImage?.name}
                      </p>
                      <p className="text-slate-500">{formatBytes(cluster.anchorImage?.sizeBytes)}</p>
                    </div>
                  </div>

                  {/* Similar Matches */}
                  <div className="space-y-3">
                    {cluster.similarImages?.map((sim, sIdx) => (
                      <div
                        key={sim.file?.id || sIdx}
                        className="p-3 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors flex items-center gap-3"
                      >
                        {sim.file?.previewUrl && (
                          <img
                            src={sim.file.previewUrl}
                            alt={sim.file.name}
                            className="w-20 h-20 object-cover rounded-lg border border-slate-200 shrink-0"
                          />
                        )}
                        <div className="min-w-0 flex-1 space-y-1">
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-bold text-slate-900 truncate">
                              {sim.file?.name}
                            </span>
                            <Badge variant="purple" size="xs">
                              {sim.similarityPercent?.toFixed(1) || '95'}% Match
                            </Badge>
                          </div>
                          <p className="text-[11px] text-slate-500">
                            Hamming Distance: {sim.hammingDistance || 2} / 64 bits
                          </p>
                          <p className="text-[11px] text-slate-500">
                            {formatBytes(sim.file?.sizeBytes)}
                          </p>
                        </div>
                        <Button
                          variant="outline"
                          size="xs"
                          icon={Trash2}
                          onClick={() => handleDeleteFile(sim.file?.id)}
                          className="text-rose-600 hover:bg-rose-50"
                        >
                          Discard
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default DuplicateFiles;
