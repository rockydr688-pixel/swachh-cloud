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
import { Clock, Archive, Trash2, Calendar } from 'lucide-react';

export const InactiveFiles = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('date');
  const [sortOrder, setSortOrder] = useState('asc'); // Oldest first
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, totalCount: 0, totalPages: 1 });

  const fetchInactiveFiles = async () => {
    setLoading(true);
    try {
      const res = await fileService.getFiles({
        category: 'inactive',
        search,
        sortBy,
        sortOrder,
        page: currentPage,
        limit: 10,
      });
      setFiles(res.data);
      setPagination(res.pagination);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInactiveFiles();
  }, [search, sortBy, sortOrder, currentPage]);

  const handleDeleteFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const handleArchiveFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Inactive & Stale Files"
        subtitle="Unused assets, completed client workspaces, and dormant temp logs untouched for more than 180 days."
        breadcrumbs={[
          { label: 'Files', path: '/files' },
          { label: 'Inactive Files' },
        ]}
        actions={
          <Badge variant="indigo" size="md">
            48.6 GB Dormant
          </Badge>
        }
      />

      {/* Summary Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-slate-50/70 border-slate-200">
          <span className="text-xs text-slate-500 font-semibold uppercase">
            Threshold Cutoff
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1">180+ Days</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Zero read/write I/O activity</p>
        </Card>

        <Card className="bg-slate-50/70 border-slate-200">
          <span className="text-xs text-slate-500 font-semibold uppercase">
            Total Inactive Files
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1">620 Files</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Consuming 48.6 GB volume</p>
        </Card>

        <Card className="bg-slate-50/70 border-slate-200">
          <span className="text-xs text-slate-500 font-semibold uppercase">
            Oldest Unaccessed File
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1">360 Days</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">legacy_project_backup_2024.tar.gz</p>
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

