import React from 'react';
import Modal from '../common/Modal';
import Badge from '../common/Badge';
import Button from '../common/Button';
import { formatBytes, formatDateTime, formatBlurScore } from '../../utils/formatters';
import { getFileIcon } from './CategoryPill';
import {
  FileText,
  Calendar,
  HardDrive,
  Hash,
  AlertTriangle,
  Trash2,
  Archive,
  CheckCircle,
  ExternalLink,
} from 'lucide-react';

export const FilePreviewModal = ({
  file,
  isOpen,
  onClose,
  onDelete,
  onArchive,
}) => {
  if (!file) return null;

  const { icon: Icon, color } = getFileIcon(file.fileType, file.extension);

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

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="File Inspection & AI Diagnostics"
      subtitle={`Detailed metadata for ${file.name}`}
      maxWidth="max-w-2xl"
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="ghost" size="sm" onClick={onClose}>
            Close
          </Button>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              icon={Archive}
              onClick={() => {
                if (onArchive) onArchive(file.id);
                onClose();
              }}
            >
              Move to Archive
            </Button>
            <Button
              variant="danger"
              size="sm"
              icon={Trash2}
              onClick={() => {
                if (onDelete) onDelete(file.id);
                onClose();
              }}
            >
              Delete File
            </Button>
          </div>
        </div>
      }
    >
      <div className="space-y-6">
        {/* Top File Summary Banner */}
        <div className="flex items-center gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200/80">
          <div className={`p-3.5 rounded-xl border ${color} shrink-0`}>
            <Icon className="w-8 h-8" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h4 className="text-sm font-bold text-slate-900 truncate">
                {file.name}
              </h4>
              <Badge variant={getStatusBadgeVariant(file.recommendationStatus)} size="xs">
                {file.recommendationStatus}
              </Badge>
            </div>
            <p className="text-xs text-slate-500 mt-1 font-mono break-all">
              {file.storageLocation}
            </p>
          </div>
        </div>

        {/* Media Preview if Image or Thumbnail */}
        {file.previewUrl ? (
          <div className="rounded-xl overflow-hidden border border-slate-200 bg-slate-950 relative group">
            <img
              src={file.previewUrl}
              alt={file.name}
              className="w-full h-64 object-cover object-center transition-transform group-hover:scale-105 duration-300"
            />
            {file.blurScore !== undefined && (
              <div className="absolute bottom-3 left-3 bg-slate-900/90 backdrop-blur-md px-3 py-1.5 rounded-lg border border-slate-700/80 text-white text-xs">
                <span className="text-slate-400">Blur Metric: </span>
                <span className="font-bold text-amber-400">
                  {formatBlurScore(file.blurScore)} (Blurry)
                </span>
              </div>
            )}
          </div>
        ) : null}

        {/* AI Detection Reason Alert */}
        <div className="p-4 rounded-xl bg-indigo-50/70 border border-indigo-100 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
          <div>
            <h5 className="text-xs font-bold text-indigo-950 uppercase tracking-wide">
              Detection Reason
            </h5>
            <p className="text-xs text-indigo-900 mt-0.5 font-medium">
              {file.detectionReason}
            </p>
          </div>
        </div>

        {/* Metadata Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
          <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/50">
            <span className="text-slate-400 font-medium block">File Size</span>
            <span className="text-slate-800 font-bold text-sm">
              {formatBytes(file.sizeBytes)} ({file.sizeBytes?.toLocaleString()} bytes)
            </span>
          </div>

          <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/50">
            <span className="text-slate-400 font-medium block">Storage Source</span>
            <span className="text-slate-800 font-bold text-sm capitalize">
              {file.storageType === 'local' ? 'Local Computer Daemon' : 'Google Drive Cloud'}
            </span>
          </div>

          <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/50">
            <span className="text-slate-400 font-medium block">Last Modified</span>
            <span className="text-slate-800 font-semibold">
              {formatDateTime(file.lastModified)}
            </span>
          </div>

          <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/50">
            <span className="text-slate-400 font-medium block">Last Accessed</span>
            <span className="text-slate-800 font-semibold">
              {formatDateTime(file.lastAccessed)}
            </span>
          </div>

          {file.hash && (
            <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/50 sm:col-span-2">
              <span className="text-slate-400 font-medium block">Cryptographic Hash</span>
              <span className="text-slate-700 font-mono text-[11px] break-all">
                {file.hash}
              </span>
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
};

export default FilePreviewModal;

