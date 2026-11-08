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
import { Maximize2, Archive, Trash2, Filter } from 'lucide-react';

export const LargeFiles = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('size');
  const [sortOrder, setSortOrder] = useState('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, totalCount: 0, totalPages: 1 });

  const fetchLargeFiles = async () => {
    setLoading(true);
    try {
      const res = await fileService.getFiles({
        category: 'large',
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
    fetchLargeFiles();
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
        title="Large Files (>500MB)"
        subtitle="Oversized virtual machine disks, 4K screen recordings, and machine learning model checkpoints consuming primary drive capacity."
        breadcrumbs={[
          { label: 'Files', path: '/files' },
          { label: 'Large Files' },
        ]}
        actions={
          <Badge variant="warning" size="md">
            112.8 GB in 185 Files
          </Badge>
        }
      />

      {/* Quick Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-amber-50/50 border-amber-200/80">
          <span className="text-xs text-amber-700 font-semibold uppercase">
            Total Space Consumed
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1">112.8 GB</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Average file size ~609 MB</p>
        </Card>

        <Card className="bg-blue-50/50 border-blue-200/80">
          <span className="text-xs text-blue-700 font-semibold uppercase">
            Largest Detected Item
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1">13.78 GB</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">client_presentation_recording_4k.mp4</p>
        </Card>

        <Card className="bg-violet-50/50 border-violet-200/80">
          <span className="text-xs text-violet-700 font-semibold uppercase">
            Archival Opportunity
          </span>
          <h4 className="text-xl font-bold text-slate-900 mt-1">78% Inactive</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Not accessed in past 90 days</p>
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
          setSortBy('size');
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

export default LargeFiles;

