import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  HardDrive,
  Sparkles,
  Files,
  Copy,
  ImageOff,
  Maximize2,
  Clock,
  ArrowRight,
  TrendingDown,
  ShieldCheck,
  AlertTriangle,
  RefreshCw,
  FolderOpen,
} from 'lucide-react';
import { useStorage } from '../context/StorageContext';
import { formatBytes, formatNumber, formatDate } from '../utils/formatters';
import PageHeader from '../components/layout/PageHeader';
import StatCard from '../components/common/StatCard';
import Button from '../components/common/Button';
import Card from '../components/common/Card';
import Badge from '../components/common/Badge';
import ProgressBar from '../components/common/ProgressBar';
import LoadingSpinner from '../components/common/LoadingSpinner';
import StorageUsageChart from '../components/charts/StorageUsageChart';
import CategoryDistributionChart from '../components/charts/CategoryDistributionChart';
import {
  mockStorageUsageHistory,
  mockCategoryDistribution,
} from '../data/mockData';

export const Dashboard = () => {
  const navigate = useNavigate();
  const { stats, recommendations, recentScans, loading, runScan, isScanning } =
    useStorage();

  if (loading || !stats) {
    return <LoadingSpinner fullPage message="Aggregating storage telemetry..." />;
  }

  const usedPercentage = Math.round(
    (stats.totalUsedBytes / stats.totalCapacityBytes) * 100
  );
  const recoverablePercentage = Math.round(
    (stats.recoverableBytes / stats.totalUsedBytes) * 100
  );

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <PageHeader
        title="Storage Intelligence Dashboard"
        subtitle="Real-time telemetry, AI bloat diagnostics, and optimization insights across your connected storage drives."
        badge={
          <Badge variant="indigo" size="sm">
            AI Engine v2.4
          </Badge>
        }
        actions={
          <>
            <Button
              variant="secondary"
              size="sm"
              icon={RefreshCw}
              onClick={() => runScan('Quick Scan')}
              isLoading={isScanning}
            >
              Refresh Telemetry
            </Button>
            <Button
              variant="primary"
              size="sm"
              icon={Sparkles}
              onClick={() => navigate('/recommendations')}
              className="bg-indigo-600 hover:bg-indigo-700 shadow-sm"
            >
              Review Recommendations
            </Button>
          </>
        }
      />

      {/* Prominent Sample Data Notice Banner */}
      <div className="p-4 rounded-xl bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-subtle">
        <div className="flex items-start gap-3">
          <div className="p-2 rounded-lg bg-amber-100 text-amber-800 shrink-0 mt-0.5">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-amber-950 uppercase tracking-wide">
              Sample Data Preview Mode
            </h4>
            <p className="text-xs text-amber-900 mt-0.5">
              Metrics below reflect realistic enterprise simulation data. Connect your local folder daemon or Google Drive in{' '}
              <button
                onClick={() => navigate('/connections')}
                className="font-bold underline hover:text-amber-950"
              >
                Storage Connections
              </button>{' '}
              to populate live production diagnostics.
            </p>
          </div>
        </div>
        <Badge variant="warning" size="sm">
          Simulated Workspace
        </Badge>
      </div>

      {/* Main Storage Capacity & Recoverable Banner */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Total Storage Card */}
        <Card className="lg:col-span-2 relative overflow-hidden bg-gradient-to-br from-slate-900 via-[#0B132B] to-slate-900 text-white border-slate-800">
          <div className="relative z-10 flex flex-col justify-between h-full p-2">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  Total Storage Pool Capacity
                </span>
                <Badge variant="navy" size="xs">
                  1 TB Total Volume
                </Badge>
              </div>

              <div className="mt-3 flex items-baseline gap-3">
                <h3 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
                  {formatBytes(stats.totalUsedBytes)}
                </h3>
                <span className="text-sm text-slate-400 font-medium">
                  used of {formatBytes(stats.totalCapacityBytes)} ({usedPercentage}%)
                </span>
              </div>

              {/* Capacity Bar */}
              <div className="mt-5 space-y-2">
                <div className="w-full bg-slate-800 h-3 rounded-full overflow-hidden p-0.5 border border-slate-700/60">
                  <div
                    className="h-full bg-gradient-to-r from-blue-500 via-indigo-500 to-violet-500 rounded-full transition-all duration-500"
                    style={{ width: `${usedPercentage}%` }}
                  />
                </div>
                <div className="flex items-center justify-between text-[11px] text-slate-400">
                  <span>Available: {formatBytes(stats.totalCapacityBytes - stats.totalUsedBytes)}</span>
                  <span>Health Score: {stats.healthScore}/100</span>
                </div>
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                Local storage agent synced 12m ago
              </span>
              <button
                onClick={() => navigate('/connections')}
                className="text-indigo-400 hover:text-indigo-300 font-semibold flex items-center gap-1"
              >
                Manage Drives <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </Card>

        {/* Recoverable Storage Highlight Card */}
        <Card className="bg-gradient-to-br from-indigo-500 to-violet-700 text-white border-0 shadow-lg shadow-indigo-600/10">
          <div className="flex flex-col justify-between h-full p-2">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-indigo-100">
                  Recoverable Savings
                </span>
                <span className="p-1.5 rounded-lg bg-white/10 backdrop-blur-md">
                  <Sparkles className="w-4 h-4 text-white" />
                </span>
              </div>

              <div className="mt-3">
                <h3 className="text-3xl font-extrabold text-white">
                  {formatBytes(stats.recoverableBytes)}
                </h3>
                <p className="text-xs text-indigo-100 font-medium mt-1">
                  ~{recoverablePercentage}% of your used drive can be safely reclaimed
                </p>
              </div>

              <div className="mt-4 p-3 bg-white/10 rounded-xl backdrop-blur-sm border border-white/10 text-xs text-indigo-50 space-y-1">
                <div className="flex justify-between">
                  <span>Exact Duplicates:</span>
                  <span className="font-bold">{formatBytes(stats.duplicateBytes)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Blurry Photos:</span>
                  <span className="font-bold">{formatBytes(stats.blurryImagesBytes)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Stale Large Files:</span>
                  <span className="font-bold">{formatBytes(stats.largeFilesBytes)}</span>
                </div>
              </div>
            </div>

            <div className="mt-5">
              <Button
                variant="navy"
                size="sm"
                className="w-full justify-center bg-white text-indigo-900 hover:bg-slate-100 font-bold shadow-md"
                onClick={() => navigate('/recommendations')}
              >
                Clean All Safe Items
              </Button>
            </div>
          </div>
        </Card>
      </div>

      {/* 4 Core Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Files Scanned"
          value={formatNumber(stats.totalFilesScanned)}
          subvalue="Across 3 local directories"
          icon={Files}
          color="blue"
          onClick={() => navigate('/files')}
        />
        <StatCard
          title="Duplicate Files"
          value={formatNumber(stats.duplicateFilesCount)}
          subvalue={formatBytes(stats.duplicateBytes)}
          icon={Copy}
          color="indigo"
          badgeText="89.2 GB"
          badgeVariant="indigo"
          onClick={() => navigate('/files/duplicates')}
        />
        <StatCard
          title="Blurry Images"
          value={formatNumber(stats.blurryImagesCount)}
          subvalue={formatBytes(stats.blurryImagesBytes)}
          icon={ImageOff}
          color="violet"
          badgeText="14.5 GB"
          badgeVariant="violet"
          onClick={() => navigate('/files/blurry')}
        />
        <StatCard
          title="Large Files (>500MB)"
          value={formatNumber(stats.largeFilesCount)}
          subvalue={formatBytes(stats.largeFilesBytes)}
          icon={Maximize2}
          color="amber"
          badgeText="112.8 GB"
          badgeVariant="warning"
          onClick={() => navigate('/files/large')}
        />
      </div>

      {/* Analytics Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <StorageUsageChart data={mockStorageUsageHistory} />
        </div>
        <div>
          <CategoryDistributionChart data={mockCategoryDistribution} />
        </div>
      </div>

      {/* Recent Cleanup Recommendations & Recent Scan Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Recommendations */}
        <Card
          title="Prioritized AI Recommendations"
          subtitle="Top automated actions to optimize storage health"
          headerAction={
            <Button
              variant="ghost"
              size="xs"
              onClick={() => navigate('/recommendations')}
              className="text-indigo-600 hover:text-indigo-700"
            >
              View All ({recommendations.length})
            </Button>
          }
        >
          <div className="space-y-3">
            {recommendations.slice(0, 3).map((rec) => (
              <div
                key={rec.id}
                className="p-3.5 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 transition-colors flex items-start justify-between gap-3"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-slate-900">
                      {rec.title}
                    </span>
                    <Badge
                      variant={
                        rec.safetyLevel === 'Safe to Delete' ? 'success' : 'warning'
                      }
                      size="xs"
                    >
                      {rec.safetyScore}% Safe
                    </Badge>
                  </div>
                  <p className="text-[11px] text-slate-500 line-clamp-2">
                    {rec.description}
                  </p>
                  <span className="text-[10px] font-bold text-indigo-600 block">
                    Save {formatBytes(rec.potentialSavingsBytes)}
                  </span>
                </div>

                <Button
                  size="xs"
                  variant="outline"
                  onClick={() => navigate('/recommendations')}
                  className="shrink-0"
                >
                  Action
                </Button>
              </div>
            ))}
          </div>
        </Card>

        {/* Recent Scanning Activity */}
        <Card
          title="Recent Scanning Activity"
          subtitle="Audited inspection runs across drives and agents"
          headerAction={
            <Button
              variant="ghost"
              size="xs"
              onClick={() => navigate('/scanner')}
              className="text-indigo-600 hover:text-indigo-700"
            >
              Scanner Logs
            </Button>
          }
        >
          <div className="space-y-3">
            {recentScans.map((scan) => (
              <div
                key={scan.id}
                className="p-3 rounded-xl border border-slate-100 flex items-center justify-between gap-3 text-xs"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-slate-100 text-slate-600 shrink-0">
                    <Clock className="w-4 h-4" />
                  </div>
                  <div>
                    <h5 className="font-semibold text-slate-900">{scan.target}</h5>
                    <div className="flex items-center gap-2 text-[11px] text-slate-400 mt-0.5">
                      <span>{scan.duration}</span>
                      <span>•</span>
                      <span>{formatNumber(scan.filesScanned)} files</span>
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <span className="font-bold text-slate-800 block">
                    {formatBytes(scan.potentialSavingsBytes)}
                  </span>
                  <Badge
                    variant={scan.status === 'completed' ? 'success' : 'default'}
                    size="xs"
                  >
                    {scan.status}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;

