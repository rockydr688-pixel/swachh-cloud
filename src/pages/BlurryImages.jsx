import React, { useState, useEffect } from 'react';
import PageHeader from '../components/layout/PageHeader';
import FileFilterToolbar from '../components/files/FileFilterToolbar';
import FileTable from '../components/files/FileTable';
import FilePreviewModal from '../components/files/FilePreviewModal';
import Pagination from '../components/common/Pagination';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import { fileService } from '../services/fileService';
import { formatBytes, formatBlurScore, formatDate } from '../utils/formatters';
import {
  ImageOff,
  LayoutGrid,
  List,
  Eye,
  Trash2,
  CheckCircle,
  AlertTriangle,
  ZoomIn,
} from 'lucide-react';

export const BlurryImages = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState('grid'); // 'grid' | 'table'
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('score');
  const [sortOrder, setSortOrder] = useState('asc');
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewOpen, setPreviewOpen] = useState(false);

  const fetchBlurryImages = async () => {
    setLoading(true);
    try {
      const res = await fileService.getFiles({
        category: 'blurry',
        search,
        sortBy,
        sortOrder,
        page: 1,
        limit: 20,
      });
      setFiles(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBlurryImages();
  }, [search, sortBy, sortOrder]);

  const handleDelete = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Blurry & Low-Quality Images"
        subtitle="Computer vision Laplacian variance analysis detecting unfocused, motion-blurred, and degraded photos."
        breadcrumbs={[
          { label: 'Files', path: '/files' },
          { label: 'Blurry Images' },
        ]}
        actions={
          <div className="flex items-center gap-2">
            <div className="p-1 bg-slate-100 rounded-lg border border-slate-200 flex items-center">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-1.5 rounded-md text-xs font-semibold flex items-center gap-1 transition-colors ${
                  viewMode === 'grid'
                    ? 'bg-white shadow-sm text-indigo-600'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                <LayoutGrid className="w-3.5 h-3.5" />
                <span>Gallery</span>
              </button>
              <button
                onClick={() => setViewMode('table')}
                className={`p-1.5 rounded-md text-xs font-semibold flex items-center gap-1 transition-colors ${
                  viewMode === 'table'
                    ? 'bg-white shadow-sm text-indigo-600'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                <List className="w-3.5 h-3.5" />
                <span>Table</span>
              </button>
            </div>
            <Button
              variant="danger"
              size="sm"
              icon={Trash2}
              onClick={() => setFiles([])}
              disabled={files.length === 0}
            >
              Discard All Blurry (14.5 GB)
            </Button>
          </div>
        }
      />

      {/* Filter and Stats */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center gap-3">
          <Badge variant="violet" size="md">
            Threshold: &lt; 85% Clarity
          </Badge>
          <span className="text-xs text-slate-500 font-medium">
            348 images flagged (14.5 GB potential reclaim)
          </span>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500 font-medium">Sort by:</span>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-700 font-medium"
          >
            <option value="score">Blur Score (Blurriest First)</option>
            <option value="size">Size (Largest First)</option>
            <option value="date">Date Taken</option>
          </select>
        </div>
      </div>

      {/* Gallery Grid or Table View */}
      {viewMode === 'grid' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {files.map((file) => (
            <Card
              key={file.id}
              noPadding
              className="overflow-hidden hover:shadow-md transition-shadow group flex flex-col justify-between"
            >
              <div>
                {/* Image Container with blur badge */}
                <div className="relative h-48 bg-slate-950 overflow-hidden cursor-pointer"
                  onClick={() => {
                    setSelectedFile(file);
                    setPreviewOpen(true);
                  }}
                >
                  {file.previewUrl ? (
                    <img
                      src={file.previewUrl}
                      alt={file.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-slate-500">
                      <ImageOff className="w-10 h-10" />
                    </div>
                  )}

                  <div className="absolute top-2.5 left-2.5">
                    <Badge variant="warning" size="xs">
                      {formatBlurScore(file.blurScore)}
                    </Badge>
                  </div>

                  <div className="absolute inset-0 bg-slate-900/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                    <span className="p-2 rounded-full bg-white text-slate-900 shadow-md">
                      <ZoomIn className="w-4 h-4" />
                    </span>
                  </div>
                </div>

                {/* Details */}
                <div className="p-4 space-y-1.5">
                  <h4
                    className="text-xs font-bold text-slate-900 truncate"
                    title={file.name}
                  >
                    {file.name}
                  </h4>
                  <div className="flex items-center justify-between text-[11px] text-slate-500">
                    <span>{formatBytes(file.sizeBytes)}</span>
                    <span>{formatDate(file.lastModified)}</span>
                  </div>
                  <p className="text-[11px] text-indigo-700 bg-indigo-50/70 p-2 rounded-lg border border-indigo-100/60 line-clamp-2">
                    {file.detectionReason}
                  </p>
                </div>
              </div>

              {/* Actions Footer */}
              <div className="p-3 bg-slate-50/80 border-t border-slate-100 flex items-center justify-between">
                <Button
                  size="xs"
                  variant="ghost"
                  onClick={() => {
                    setSelectedFile(file);
                    setPreviewOpen(true);
                  }}
                >
                  Inspect
                </Button>
                <Button
                  size="xs"
                  variant="danger"
                  icon={Trash2}
                  onClick={() => handleDelete(file.id)}
                >
                  Discard
                </Button>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <FileTable
          files={files}
          loading={loading}
          sortBy={sortBy}
          sortOrder={sortOrder}
          onSortChange={(s, o) => {
            setSortBy(s);
            setSortOrder(o);
          }}
          onDeleteFile={handleDelete}
        />
      )}

      {/* File Preview Modal */}
      <FilePreviewModal
        file={selectedFile}
        isOpen={previewOpen}
        onClose={() => setPreviewOpen(false)}
        onDelete={handleDelete}
      />
    </div>
  );
};

export default BlurryImages;

