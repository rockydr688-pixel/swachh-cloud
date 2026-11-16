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
import {
  Maximize2,
  Archive,
  Trash2,
  Filter,
  SlidersHorizontal,
  Video,
  Image as ImageIcon,
  FileArchive,
  FileText,
  HelpCircle,
} from 'lucide-react';

export const LargeFiles = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState('all'); // 'all' | 'image' | 'video' | 'archive' | 'document' | 'other'
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('size');
  const [sortOrder, setSortOrder] = useState('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, totalCount: 0, totalPages: 1 });
  const [showConfig, setShowConfig] = useState(false);

  // Configurable thresholds in MB
  const [thresholds, setThresholds] = useState({
    image: 25,
    video: 500,
    archive: 1024,
    document: 100,
    other: 500,
  });

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

      let items = res.data || [];
      if (selectedCategory !== 'all') {
        items = items.filter((f) => {
          const type = (f.fileType || '').toLowerCase();
          if (selectedCategory === 'image') return type === 'image';
          if (selectedCategory === 'video') return type === 'video';
          if (selectedCategory === 'archive') return type === 'archive';
          if (selectedCategory === 'document') return type === 'document';
          return type !== 'image' && type !== 'video' && type !== 'archive' && type !== 'document';
        });
      }

      setFiles(items);
      setPagination(res.pagination);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLargeFiles();
  }, [selectedCategory, search, sortBy, sortOrder, currentPage]);

  const handleDeleteFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const handleArchiveFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Categorized Large File Detection"
        subtitle="Identifies storage-heavy assets categorized into images, 4K videos, archives, and virtual disks with customizable thresholds."
        breadcrumbs={[
          { label: 'Files', path: '/files' },
          { label: 'Large Files' },
        ]}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              icon={SlidersHorizontal}
              onClick={() => setShowConfig(!showConfig)}
            >
              Configure Thresholds
            </Button>
            <Badge variant="warning" size="md">
              112.8 GB in 185 Files
            </Badge>
          </div>
        }
      />

      {/* Thresholds Customization Drawer */}
      {showConfig && (
        <Card className="p-4 bg-slate-50 border-indigo-200">
          <div className="flex items-center justify-between mb-3">
            <h4 className="text-xs font-bold text-slate-900 uppercase">
              Custom Size Thresholds (MB)
            </h4>
            <span className="text-[11px] text-slate-500">
              Files exceeding these category thresholds will be flagged
            </span>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
            <div>
              <label className="text-[11px] font-semibold text-slate-700 block mb-1">
                Images (MB)
              </label>
              <input
                type="number"
                value={thresholds.image}
                onChange={(e) => setThresholds({ ...thresholds, image: Number(e.target.value) })}
                className="w-full text-xs p-1.5 border border-slate-300 rounded bg-white"
              />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-slate-700 block mb-1">
                Videos (MB)
              </label>
              <input
                type="number"
                value={thresholds.video}
                onChange={(e) => setThresholds({ ...thresholds, video: Number(e.target.value) })}
                className="w-full text-xs p-1.5 border border-slate-300 rounded bg-white"
              />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-slate-700 block mb-1">
                Archives (MB)
              </label>
              <input
                type="number"
                value={thresholds.archive}
                onChange={(e) => setThresholds({ ...thresholds, archive: Number(e.target.value) })}
                className="w-full text-xs p-1.5 border border-slate-300 rounded bg-white"
              />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-slate-700 block mb-1">
                Documents (MB)
              </label>
              <input
                type="number"
                value={thresholds.document}
                onChange={(e) => setThresholds({ ...thresholds, document: Number(e.target.value) })}
                className="w-full text-xs p-1.5 border border-slate-300 rounded bg-white"
              />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-slate-700 block mb-1">
                Other Files (MB)
              </label>
              <input
                type="number"
                value={thresholds.other}
                onChange={(e) => setThresholds({ ...thresholds, other: Number(e.target.value) })}
                className="w-full text-xs p-1.5 border border-slate-300 rounded bg-white"
              />
            </div>
          </div>
        </Card>
      )}

      {/* Category Tabs */}
      <div className="flex border-b border-slate-200 overflow-x-auto">
        <button
          onClick={() => {
            setSelectedCategory('all');
            setCurrentPage(1);
          }}
          className={`py-3 px-4 text-xs font-bold border-b-2 whitespace-nowrap transition-all ${
            selectedCategory === 'all'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          All Large Files
        </button>
        <button
          onClick={() => {
            setSelectedCategory('video');
            setCurrentPage(1);
          }}
          className={`flex items-center gap-1.5 py-3 px-4 text-xs font-bold border-b-2 whitespace-nowrap transition-all ${
            selectedCategory === 'video'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Video className="w-3.5 h-3.5" />
          <span>Videos (&gt; {thresholds.video}MB)</span>
        </button>
        <button
          onClick={() => {
            setSelectedCategory('archive');
            setCurrentPage(1);
          }}
          className={`flex items-center gap-1.5 py-3 px-4 text-xs font-bold border-b-2 whitespace-nowrap transition-all ${
            selectedCategory === 'archive'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <FileArchive className="w-3.5 h-3.5" />
          <span>Archives (&gt; {thresholds.archive}MB)</span>
        </button>
        <button
          onClick={() => {
            setSelectedCategory('image');
            setCurrentPage(1);
          }}
          className={`flex items-center gap-1.5 py-3 px-4 text-xs font-bold border-b-2 whitespace-nowrap transition-all ${
            selectedCategory === 'image'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ImageIcon className="w-3.5 h-3.5" />
          <span>Images (&gt; {thresholds.image}MB)</span>
        </button>
        <button
          onClick={() => {
            setSelectedCategory('document');
            setCurrentPage(1);
          }}
          className={`flex items-center gap-1.5 py-3 px-4 text-xs font-bold border-b-2 whitespace-nowrap transition-all ${
            selectedCategory === 'document'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <FileText className="w-3.5 h-3.5" />
          <span>Documents (&gt; {thresholds.document}MB)</span>
        </button>
        <button
          onClick={() => {
            setSelectedCategory('other');
            setCurrentPage(1);
          }}
          className={`py-3 px-4 text-xs font-bold border-b-2 whitespace-nowrap transition-all ${
            selectedCategory === 'other'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          Other Files (&gt; {thresholds.other}MB)
        </button>
      </div>

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
          setSortOrder('desc');
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
