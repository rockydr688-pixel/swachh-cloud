import React from 'react';
import PageHeader from '../components/layout/PageHeader';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import StorageUsageChart from '../components/charts/StorageUsageChart';
import CategoryDistributionChart from '../components/charts/CategoryDistributionChart';
import {
  mockStorageUsageHistory,
  mockCategoryDistribution,
  mockStorageStats,
} from '../data/mockData';
import { formatBytes } from '../utils/formatters';
import {
  BarChart3,
  Download,
  TrendingDown,
  DollarSign,
  Calendar,
  Layers,
  ArrowUpRight,
  HardDrive,
} from 'lucide-react';

export const ReportsAnalytics = () => {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Reports & Storage Analytics"
        subtitle="Capacity forecasting, cloud tier expenditure savings, and historical data velocity metrics."
        breadcrumbs={[{ label: 'Reports & Analytics' }]}
        actions={
          <Button
            variant="secondary"
            size="sm"
            icon={Download}
            onClick={() => window.print()}
          >
            Export PDF Report
          </Button>
        }
      />

      {/* ROI & Key Analytics Callouts */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="bg-gradient-to-br from-indigo-50 to-white border-indigo-100">
          <div className="flex items-center justify-between">
            <span className="text-xs text-indigo-700 font-semibold uppercase">
              Cloud Cost Savings
            </span>
            <div className="p-2 rounded-lg bg-indigo-100 text-indigo-700">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <h4 className="text-2xl font-bold text-slate-900 mt-2">$240 / yr</h4>
          <p className="text-[11px] text-slate-500 mt-1">
            Prevented Google Workspace 2TB tier upgrade
          </p>
        </Card>

        <Card className="bg-gradient-to-br from-violet-50 to-white border-violet-100">
          <div className="flex items-center justify-between">
            <span className="text-xs text-violet-700 font-semibold uppercase">
              Storage Velocity
            </span>
            <div className="p-2 rounded-lg bg-violet-100 text-violet-700">
              <TrendingDown className="w-4 h-4" />
            </div>
          </div>
          <h4 className="text-2xl font-bold text-slate-900 mt-2">+24 GB / mo</h4>
          <p className="text-[11px] text-slate-500 mt-1">
            Average monthly accumulation rate
          </p>
        </Card>

        <Card className="bg-gradient-to-br from-blue-50 to-white border-blue-100">
          <div className="flex items-center justify-between">
            <span className="text-xs text-blue-700 font-semibold uppercase">
              Estimated Runway
            </span>
            <div className="p-2 rounded-lg bg-blue-100 text-blue-700">
              <Calendar className="w-4 h-4" />
            </div>
          </div>
          <h4 className="text-2xl font-bold text-slate-900 mt-2">14 Months</h4>
          <p className="text-[11px] text-slate-500 mt-1">
            With regular AI optimization applied
          </p>
        </Card>

        <Card className="bg-gradient-to-br from-emerald-50 to-white border-emerald-100">
          <div className="flex items-center justify-between">
            <span className="text-xs text-emerald-700 font-semibold uppercase">
              Clean Efficiency
            </span>
            <div className="p-2 rounded-lg bg-emerald-100 text-emerald-700">
              <Layers className="w-4 h-4" />
            </div>
          </div>
          <h4 className="text-2xl font-bold text-slate-900 mt-2">98.4%</h4>
          <p className="text-[11px] text-slate-500 mt-1">
            Zero false positives recorded
          </p>
        </Card>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <StorageUsageChart data={mockStorageUsageHistory} />
        </div>
        <div>
          <CategoryDistributionChart data={mockCategoryDistribution} />
        </div>
      </div>

      {/* Storage Health & Optimization Insights */}
      <Card
        title="Optimization Recommendations by Storage Medium"
        subtitle="Comparative analysis across internal SSD and cloud repositories"
      >
        <div className="space-y-3 text-xs">
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex items-start justify-between gap-4">
            <div className="space-y-1">
              <span className="font-bold text-slate-900 text-sm">
                Local Computer SSD (C:/ & D:/ Drives)
              </span>
              <p className="text-slate-600">
                Downloads folder contains 1,482 duplicate binaries and archives. Purging redundant installers will immediately recover 89.2 GB of ultra-fast NVMe space.
              </p>
            </div>
            <Badge variant="indigo" size="sm">
              Priority High
            </Badge>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex items-start justify-between gap-4">
            <div className="space-y-1">
              <span className="font-bold text-slate-900 text-sm">
                Google Drive Cloud Repository
              </span>
              <p className="text-slate-600">
                Pending backend OAuth connection. Once connected, CloudClean AI will index shared team drives and identify unversioned PDF revisions and orphaned assets.
              </p>
            </div>
            <Badge variant="warning" size="sm">
              OAuth Required
            </Badge>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default ReportsAnalytics;

