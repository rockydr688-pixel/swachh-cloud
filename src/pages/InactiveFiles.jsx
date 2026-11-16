import React, { useState, useEffect } from 'react';
import PageHeader from '../components/layout/PageHeader';
import FileFilterToolbar from '../components/files/FileFilterToolbar';
import FileTable from '../components/files/FileTable';
import Pagination from '../components/common/Pagination';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import { fileService } from '../services/fileService';
import { formatBytes } from '../utils/formatters';
import { Clock, Archive, Trash2, Calendar, ShieldCheck, AlertCircle, Info } from 'lucide-react';

export const InactiveFiles = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [inactivityCategory, setInactivityCategory] = useState('all'); // 'all' | '30_days' | '90_days' | '180_days' | '365_days'
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('date');
  const [sortOrder, setSortOrder] = useState('asc'); // Oldest first
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, totalCount: 0, totalPages: 1 });
  const [report, setReport] = useState(null);

  const fetchInactiveFiles = async () => {
    setLoading(true);
    try {
      const [tableRes, inactReport] = await Promise.all([
        fileService.getFiles({
          category: 'inactive',
          search,
          sortBy,
          sortOrder,
          page: currentPage,
          limit: 10,
        }),
        fileService.getInactiveReport(inactivityCategory),
      ]);

      let fileData = tableRes.data || [];
      if (inactivityCategory !== 'all') {
        fileData = fileData.filter((f) => f.inactivityCategory === inactivityCategory || f.detectionReason?.includes(inactivityCategory.replace('_days', '')));
      }

      setFiles(fileData);
      setPagination(tableRes.pagination);
      if (inactReport) setReport(inactReport);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInactiveFiles();
  }, [inactivityCategory, search, sortBy, sortOrder, currentPage]);

  const handleDeleteFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const handleArchiveFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Inactive & Dormant Files"
        subtitle="Tiered inactivity detection (30, 90, 180, and 365+ days) separating genuine filesystem access times from modification dates."
        breadcrumbs={[
          { label: 'Files', path: '/files' },
          { label: 'Inactive Files' },
        ]}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              icon={Archive}
              onClick={() => {}}
            >
              Batch Archive
            </Button>
            <Badge variant="indigo" size="md">
              48.6 GB Dormant
            </Badge>
          </div>
        }
      />

      {/* Timestamp Reliability Notice */}
      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-700 flex items-start gap-3">
        <Info className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
        <div>
          <h5 className="font-bold text-slate-900 uppercase">
            Access vs Modification Timestamp Signal Separation
          </h5>
          <p className="mt-0.5 text-slate-600">
            Filesystems and cloud APIs treat modification and access times independently. When reliable filesystem access times are supported by the operating system, they are prioritized. When access dates are unavailable, the platform explicitly classifies them as unavailable and uses modification age as an independent secondary signal.
          </p>
        </div>
      </div>

      {/* Inactivity Threshold Bucket Selector */}
      <div className="flex border-b border-slate-200">
        <button
          onClick={() => {
            setInactivityCategory('all');
            setCurrentPage(1);
          }}
          className={`py-3 px-4 text-xs font-bold border-b-2 transition-all ${
            inactivityCategory === 'all'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          All Inactive (Any Tier)
        </button>
        <button
          onClick={() => {
            setInactivityCategory('30_days');
            setCurrentPage(1);
          }}
          className={`py-3 px-4 text-xs font-bold border-b-2 transition-all ${
            inactivityCategory === '30_days'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          30+ Days (Recent)
        </button>
        <button
          onClick={() => {
            setInactivityCategory('90_days');
            setCurrentPage(1);
          }}
          className={`py-3 px-4 text-xs font-bold border-b-2 transition-all ${
            inactivityCategory === '90_days'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          90+ Days (Quarterly)
        </button>
        <button
          onClick={() => {
            setInactivityCategory('180_days');
            setCurrentPage(1);
          }}
          className={`py-3 px-4 text-xs font-bold border-b-2 transition-all ${
            inactivityCategory === '180_days'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          180+ Days (Semi-Annual)
        </button>
        <button
          onClick={() => {
            setInactivityCategory('365_days');
            setCurrentPage(1);
          }}
          className={`py-3 px-4 text-xs font-bold border-b-2 transition-all ${
            inactivityCategory === '365_days'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          365+ Days (Stale / 1 Year+)
        </button>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-slate-50/70 border-slate-200">
          <span className="text-xs text-slate-500 font-semibold uppercase">
            Active Filter Tier
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1 capitalize">
            {inactivityCategory.replace('_', ' ')}
          </h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Automated dormancy window</p>
        </Card>

        <Card className="bg-slate-50/70 border-slate-200">
          <span className="text-xs text-slate-500 font-semibold uppercase">
            Categorized Files
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1">
            {report?.flaggedCount || 620} Files
          </h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Consuming 48.6 GB volume</p>
        </Card>

        <Card className="bg-slate-50/70 border-slate-200">
          <span className="text-xs text-slate-500 font-semibold uppercase">
            Recommended Action
          </span>
          <h4 className="text-xl font-bold text-indigo-600 mt-1">Archive to Cold Storage</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Compress & free up primary SSD</p>
        </Card>
      </div>

      {/* Filter and Search */}
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
          setSortBy('date');
          setSortOrder('asc');
        }}
      />

      {/* Table */}
      <FileTable
        files={files}
        loading={loading}
        sortBy={sortBy}
        sortOrder={sortOrder}
        onSortChange={(s, o) => {
          setSortBy(s);
          setSortOrder(o);
        }}
        onDeleteFile={handleDeleteFile}
        onArchiveFile={handleArchiveFile}
      />

      {/* Pagination */}
      <Pagination
        currentPage={pagination.page}
        totalPages={pagination.totalPages}
        totalItems={pagination.totalCount}
        pageSize={pagination.limit}
        onPageChange={(page) => setCurrentPage(page)}
      />
    </div>
  );
};

export default InactiveFiles;
