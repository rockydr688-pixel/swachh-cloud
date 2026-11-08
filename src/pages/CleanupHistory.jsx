import React, { useState } from 'react';
import PageHeader from '../components/layout/PageHeader';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import SearchInput from '../components/common/SearchInput';
import { mockCleanupHistory } from '../data/mockData';
import { formatBytes, formatDateTime, formatNumber } from '../utils/formatters';
import {
  History,
  Download,
  CheckCircle2,
  Filter,
  ShieldCheck,
  FileCheck,
} from 'lucide-react';

export const CleanupHistory = () => {
  const [history, setHistory] = useState(mockCleanupHistory);
  const [search, setSearch] = useState('');
  const [filterCategory, setFilterCategory] = useState('all');

  const filteredHistory = history.filter((item) => {
    const matchCategory =
      filterCategory === 'all' ||
      item.targetCategory.toLowerCase() === filterCategory.toLowerCase();
    const matchSearch =
      !search ||
      item.action.toLowerCase().includes(search.toLowerCase()) ||
      item.executedBy.toLowerCase().includes(search.toLowerCase()) ||
      item.method.toLowerCase().includes(search.toLowerCase());
    return matchCategory && matchSearch;
  });

  const totalFreed = history.reduce((sum, h) => sum + h.freedSpaceBytes, 0);
  const totalFiles = history.reduce((sum, h) => sum + h.filesAffected, 0);

  const handleExportCSV = () => {
    const headers = ['ID,Timestamp,Action,Category,FilesAffected,FreedSpaceBytes,ExecutedBy,Method'];
    const rows = filteredHistory.map(
      (h) =>
        `"${h.id}","${h.timestamp}","${h.action}","${h.targetCategory}",${h.filesAffected},${h.freedSpaceBytes},"${h.executedBy}","${h.method}"`
    );
    const blob = new Blob([[...headers, ...rows].join('\n')], {
      type: 'text/csv;charset=utf-8;',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `cloudclean_audit_log_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Cleanup Execution History & Audit Trail"
        subtitle="Cryptographically verified ledger of files removed, archived, or trashed by CloudClean AI."
        breadcrumbs={[{ label: 'Cleanup History' }]}
        actions={
          <Button
            variant="secondary"
            size="sm"
            icon={Download}
            onClick={handleExportCSV}
          >
            Export Audit CSV
          </Button>
        }
      />

      {/* Aggregate Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-emerald-50/50 border-emerald-200">
          <span className="text-xs text-emerald-700 font-semibold uppercase">
            Cumulative Storage Reclaimed
          </span>
          <h4 className="text-2xl font-bold text-slate-900 mt-1">
            {formatBytes(totalFreed)}
          </h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Across 4 execution sprints</p>
        </Card>

        <Card className="bg-indigo-50/50 border-indigo-200">
          <span className="text-xs text-indigo-700 font-semibold uppercase">
            Total Files Purged / Archived
          </span>
          <h4 className="text-2xl font-bold text-slate-900 mt-1">
            {formatNumber(totalFiles)} Files
          </h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Preserved in recycle bin where applicable</p>
        </Card>

        <Card className="bg-violet-50/50 border-violet-200">
          <span className="text-xs text-violet-700 font-semibold uppercase">
            Audit Integrity Status
          </span>
          <h4 className="text-2xl font-bold text-slate-900 mt-1">100% Verified</h4>
          <p className="text-[11px] text-slate-500 mt-0.5">Zero corruption or accidental deletion</p>
        </Card>
      </div>

      {/* Filter and Search */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="w-full sm:w-80">
          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Search audit actions, operators..."
          />
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500 font-medium">Category:</span>
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-700 font-medium"
          >
            <option value="all">All Categories</option>
            <option value="Duplicates">Duplicates</option>
            <option value="Blurry Images">Blurry Images</option>
            <option value="Large Files">Large Files</option>
            <option value="Inactive Files">Inactive Files</option>
          </select>
        </div>
      </div>

      {/* History Table */}
      <Card noPadding>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                <th className="py-3 px-4">Action & Description</th>
                <th className="py-3 px-3">Category</th>
                <th className="py-3 px-3">Date & Time</th>
                <th className="py-3 px-3">Files Affected</th>
                <th className="py-3 px-3">Freed Space</th>
                <th className="py-3 px-3">Method</th>
                <th className="py-3 px-4 text-right">Executed By</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {filteredHistory.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                  <td className="py-3.5 px-4 font-semibold text-slate-900">
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>{item.action}</span>
                    </div>
                  </td>
                  <td className="py-3.5 px-3">
                    <Badge variant="navy" size="xs">
                      {item.targetCategory}
                    </Badge>
                  </td>
                  <td className="py-3.5 px-3 text-slate-500 whitespace-nowrap">
                    {formatDateTime(item.timestamp)}
                  </td>
                  <td className="py-3.5 px-3 font-semibold">
                    {formatNumber(item.filesAffected)}
                  </td>
                  <td className="py-3.5 px-3 font-bold text-emerald-600 whitespace-nowrap">
                    {formatBytes(item.freedSpaceBytes)}
                  </td>
                  <td className="py-3.5 px-3 font-mono text-[11px] text-slate-600">
                    {item.method}
                  </td>
                  <td className="py-3.5 px-4 text-right text-slate-600">
                    {item.executedBy}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};

export default CleanupHistory;

