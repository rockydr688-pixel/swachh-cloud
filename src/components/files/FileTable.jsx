import React, { useState } from 'react';
import {
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Eye,
  Trash2,
  Archive,
  Info,
  Check,
  CheckSquare,
  Square,
  X,
} from 'lucide-react';
import { formatBytes, formatDate } from '../../utils/formatters';
import CategoryPill from './CategoryPill';
import Badge from '../common/Badge';
import Button from '../common/Button';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import FilePreviewModal from './FilePreviewModal';
import { useToast } from '../../context/ToastContext';

export const FileTable = ({
  files = [],
  loading = false,
  sortBy = 'size',
  sortOrder = 'desc',
  onSortChange,
  onDeleteFile,
  onArchiveFile,
}) => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const { toast } = useToast();

  const handleSort = (field) => {
    if (!onSortChange) return;
    if (sortBy === field) {
      onSortChange(field, sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      onSortChange(field, 'desc');
    }
  };

  const renderSortIcon = (field) => {
    if (sortBy !== field) {
      return (
        <ArrowUpDown className="w-3.5 h-3.5 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity" />
      );
    }
    return sortOrder === 'asc' ? (
      <ArrowUp className="w-3.5 h-3.5 text-indigo-600" />
    ) : (
      <ArrowDown className="w-3.5 h-3.5 text-indigo-600" />
    );
  };

  const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'Safe to Delete':
        return 'success';
      case 'Review & Delete':
        return 'warning';
      case 'Recommended for Archival':
        return 'indigo';
      case 'Keep Original':
        return 'primary';
      default:
        return 'default';
    }
  };

  // Row selection handlers
  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedIds(new Set(files.map((f) => f.id)));
    } else {
      setSelectedIds(new Set());
    }
  };

  const handleToggleRow = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  // Batch actions
  const selectedFilesList = files.filter((f) => selectedIds.has(f.id));
  const selectedTotalBytes = selectedFilesList.reduce(
    (acc, f) => acc + (f.sizeBytes || 0),
    0
  );

  const handleBatchDelete = () => {
    const count = selectedIds.size;
    const freed = formatBytes(selectedTotalBytes);
    selectedIds.forEach((id) => {
      if (onDeleteFile) onDeleteFile(id);
    });
    setSelectedIds(new Set());
    toast.success(
      'Batch Deletion Completed',
      `Moved ${count} files (${freed}) to trash safety net.`
    );
  };

  const handleBatchArchive = () => {
    const count = selectedIds.size;
    const saved = formatBytes(selectedTotalBytes);
    selectedIds.forEach((id) => {
      if (onArchiveFile) onArchiveFile(id);
    });
    setSelectedIds(new Set());
    toast.info(
      'Batch Archival Staged',
      `Compressed ${count} assets (${saved}) to cold archival storage.`
    );
  };

  if (loading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200/80 p-8 shadow-sm">
        <LoadingSpinner message="Scanning and retrieving file table..." />
      </div>
    );
  }

  if (!files || files.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-slate-200/80 p-6 shadow-sm">
        <EmptyState
          title="No files match your query"
          description="Adjust your search keywords or filter settings to view indexed storage files."
        />
      </div>
    );
  }

  const isAllSelected = files.length > 0 && selectedIds.size === files.length;
  const isPartiallySelected = selectedIds.size > 0 && selectedIds.size < files.length;

  return (
    <>
      <div className="bg-white rounded-xl border border-slate-200/80 shadow-sm overflow-hidden relative">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                {/* Select All Checkbox */}
                <th className="py-3 px-3 w-10 text-center">
                  <input
                    type="checkbox"
                    checked={isAllSelected}
                    ref={(el) => {
                      if (el) el.indeterminate = isPartiallySelected;
                    }}
                    onChange={handleSelectAll}
                    className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4 cursor-pointer"
                  />
                </th>
                <th
                  onClick={() => handleSort('name')}
                  className="py-3 px-3 cursor-pointer hover:bg-slate-100 transition-colors group select-none min-w-[240px]"
                >
                  <div className="flex items-center gap-1.5">
                    <span>File Name</span>
                    {renderSortIcon('name')}
                  </div>
                </th>
                <th className="py-3 px-3 min-w-[100px]">Type</th>
                <th
                  onClick={() => handleSort('size')}
                  className="py-3 px-3 cursor-pointer hover:bg-slate-100 transition-colors group select-none min-w-[110px]"
                >
                  <div className="flex items-center gap-1.5">
                    <span>Size</span>
                    {renderSortIcon('size')}
                  </div>
                </th>
                <th className="py-3 px-3 min-w-[200px]">Storage Location</th>
                <th
                  onClick={() => handleSort('date')}
                  className="py-3 px-3 cursor-pointer hover:bg-slate-100 transition-colors group select-none min-w-[120px]"
                >
                  <div className="flex items-center gap-1.5">
                    <span>Modified</span>
                    {renderSortIcon('date')}
                  </div>
                </th>
                <th className="py-3 px-3 min-w-[220px]">Detection Reason</th>
                <th className="py-3 px-3 min-w-[150px]">Recommendation</th>
                <th className="py-3 px-4 text-right min-w-[100px]">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
              {files.map((file) => {
                const isSelected = selectedIds.has(file.id);
                return (
                  <tr
                    key={file.id}
                    className={`transition-colors group ${
                      isSelected
                        ? 'bg-indigo-50/40 hover:bg-indigo-50/60'
                        : 'hover:bg-slate-50/80'
                    }`}
                  >
                    {/* Checkbox */}
                    <td className="py-3 px-3 text-center">
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => handleToggleRow(file.id)}
                        className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4 cursor-pointer"
                      />
                    </td>

                    {/* File Name + quick preview trigger */}
                    <td className="py-3 px-3">
                      <div className="flex items-center gap-2">
                        <div
                          onClick={() => {
                            setSelectedFile(file);
                            setPreviewOpen(true);
                          }}
                          className="cursor-pointer group-hover:text-indigo-600 transition-colors font-medium text-slate-900 line-clamp-1 max-w-xs"
                          title={file.name}
                        >
                          {file.name}
                        </div>
                      </div>
                    </td>

                    {/* Type */}
                    <td className="py-3 px-3">
                      <CategoryPill
                        fileType={file.fileType}
                        extension={file.extension}
                        showLabel={false}
                      />
                    </td>

                    {/* Size */}
                    <td className="py-3 px-3 font-semibold text-slate-800 whitespace-nowrap">
                      {formatBytes(file.sizeBytes)}
                    </td>

                    {/* Storage Location */}
                    <td
                      className="py-3 px-3 font-mono text-[11px] text-slate-500 truncate max-w-xs"
                      title={file.storageLocation}
                    >
                      {file.storageLocation}
                    </td>

                    {/* Last Modified */}
                    <td className="py-3 px-3 text-slate-500 whitespace-nowrap">
                      {formatDate(file.lastModified)}
                    </td>

                    {/* Detection Reason */}
                    <td className="py-3 px-3">
                      <span
                        className="text-[11px] text-slate-600 line-clamp-1"
                        title={file.detectionReason}
                      >
                        {file.detectionReason}
                      </span>
                    </td>

                    {/* Recommendation Status */}
                    <td className="py-3 px-3 whitespace-nowrap">
                      <Badge
                        variant={getStatusBadgeVariant(file.recommendationStatus)}
                        size="xs"
                      >
                        {file.recommendationStatus}
                      </Badge>
                    </td>

                    {/* Actions */}
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1 opacity-80 group-hover:opacity-100">
                        <button
                          onClick={() => {
                            setSelectedFile(file);
                            setPreviewOpen(true);
                          }}
                          title="Inspect file"
                          className="p-1 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded transition-colors"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => {
                            if (onDeleteFile) onDeleteFile(file.id);
                            toast.warning(
                              'File Moved to Trash',
                              `Removed ${file.name} (${formatBytes(file.sizeBytes)}).`
                            );
                          }}
                          title="Delete file"
                          className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded transition-colors"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Floating Sticky Batch Action Bar */}
      {selectedIds.size > 0 && (
        <div className="fixed bottom-6 left-1/2 transform -translate-x-1/2 z-40 bg-slate-900 text-white px-5 py-3 rounded-2xl shadow-2xl border border-slate-700/80 flex items-center gap-4 sm:gap-6 animate-slide-up">
          <div className="flex items-center gap-2 text-xs">
            <span className="font-bold text-white bg-indigo-600 px-2 py-0.5 rounded-full">
              {selectedIds.size}
            </span>
            <span>files selected</span>
            <span className="text-slate-400 font-mono">
              ({formatBytes(selectedTotalBytes)})
            </span>
          </div>

          <div className="h-4 w-px bg-slate-700" />

          <div className="flex items-center gap-2">
            <Button
              variant="subtleViolet"
              size="xs"
              icon={Archive}
              onClick={handleBatchArchive}
              className="bg-slate-800 text-slate-200 hover:bg-slate-700 border-slate-700"
            >
              Batch Archive
            </Button>
            <Button
              variant="danger"
              size="xs"
              icon={Trash2}
              onClick={handleBatchDelete}
            >
              Batch Delete
            </Button>
            <button
              onClick={() => setSelectedIds(new Set())}
              className="text-slate-400 hover:text-white p-1 ml-1"
              title="Deselect all"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* File Preview Modal */}
      <FilePreviewModal
        file={selectedFile}
        isOpen={previewOpen}
        onClose={() => setPreviewOpen(false)}
        onDelete={(id) => {
          if (onDeleteFile) onDeleteFile(id);
          toast.warning('File Trashed', 'File moved to OS recycle bin.');
        }}
        onArchive={(id) => {
          if (onArchiveFile) onArchiveFile(id);
          toast.info('File Archived', 'File compressed into archive tier.');
        }}
      />
    </>
  );
};

export default FileTable;
