import React from 'react';
import {
  FileText,
  FileImage,
  FileVideo,
  FileArchive,
  Cpu,
  FileCode,
  FileSpreadsheet,
  File,
} from 'lucide-react';

export const getFileIcon = (fileType, extension) => {
  const ext = (extension || '').toLowerCase();
  const type = (fileType || '').toLowerCase();

  if (type === 'image' || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp'].includes(ext)) {
    return { icon: FileImage, color: 'text-violet-500 bg-violet-50 border-violet-200' };
  }
  if (type === 'video' || ['mp4', 'mov', 'avi', 'mkv', 'webm'].includes(ext)) {
    return { icon: FileVideo, color: 'text-blue-500 bg-blue-50 border-blue-200' };
  }
  if (type === 'archive' || ['zip', 'tar', 'gz', 'iso', 'rar', '7z'].includes(ext)) {
    return { icon: FileArchive, color: 'text-amber-500 bg-amber-50 border-amber-200' };
  }
  if (['xls', 'xlsx', 'csv'].includes(ext)) {
    return { icon: FileSpreadsheet, color: 'text-emerald-500 bg-emerald-50 border-emerald-200' };
  }
  if (['exe', 'dmg', 'app', 'msi', 'bin', 'ckpt'].includes(ext) || type === 'application' || type === 'data') {
    return { icon: Cpu, color: 'text-rose-500 bg-rose-50 border-rose-200' };
  }
  if (['pdf', 'doc', 'docx', 'txt', 'md'].includes(ext) || type === 'document') {
    return { icon: FileText, color: 'text-indigo-500 bg-indigo-50 border-indigo-200' };
  }
  return { icon: File, color: 'text-slate-500 bg-slate-50 border-slate-200' };
};

export const CategoryPill = ({ fileType, extension, showLabel = true }) => {
  const { icon: Icon, color } = getFileIcon(fileType, extension);

  return (
    <div className="flex items-center gap-1.5">
      <div className={`p-1.5 rounded-lg border ${color} shrink-0`}>
        <Icon className="w-3.5 h-3.5" />
      </div>
      {showLabel && (
        <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
          {extension || fileType || 'FILE'}
        </span>
      )}
    </div>
  );
};

export default CategoryPill;

