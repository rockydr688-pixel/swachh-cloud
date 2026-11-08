import React, { useState } from 'react';
import PageHeader from '../components/layout/PageHeader';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import { useAuth } from '../context/AuthContext';
import { mockUserSettings } from '../data/mockData';
import {
  Settings,
  User,
  Sliders,
  Bell,
  Shield,
  Save,
  CheckCircle2,
  Terminal,
} from 'lucide-react';

export const UserSettings = () => {
  const { user, updateProfile } = useAuth();
  const [profile, setProfile] = useState({
    name: user?.name || mockUserSettings.profile.fullName,
    email: user?.email || mockUserSettings.profile.email,
    role: user?.role || mockUserSettings.profile.role,
    company: mockUserSettings.profile.company,
  });

  const [scannerConfig, setScannerConfig] = useState(mockUserSettings.scannerConfig);
  const [notifications, setNotifications] = useState(mockUserSettings.notifications);
  const [savedMessage, setSavedMessage] = useState(null);

  const handleSave = (e) => {
    e.preventDefault();
    updateProfile({ name: profile.name, email: profile.email, role: profile.role });
    setSavedMessage('Preferences and diagnostic rules successfully saved.');
    setTimeout(() => setSavedMessage(null), 4000);
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <PageHeader
        title="Settings & Platform Preferences"
        subtitle="Manage your administrative credentials, AI heuristic sensitivity thresholds, and notifications."
        breadcrumbs={[{ label: 'User Settings' }]}
        actions={
          <Button
            variant="primary"
            size="md"
            icon={Save}
            onClick={handleSave}
            className="bg-indigo-600 hover:bg-indigo-700"
          >
            Save Changes
          </Button>
        }
      />

      {savedMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 flex items-center gap-2 font-semibold">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>{savedMessage}</span>
        </div>
      )}

      {/* User Profile */}
      <Card
        title="Administrator Profile"
        subtitle="Your CloudClean AI session identity"
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1.5">
              Full Name
            </label>
            <input
              type="text"
              value={profile.name}
              onChange={(e) => setProfile({ ...profile, name: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1.5">
              Email Address
            </label>
            <input
              type="email"
              value={profile.email}
              onChange={(e) => setProfile({ ...profile, email: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1.5">
              Access Role
            </label>
            <input
              type="text"
              value={profile.role}
              onChange={(e) => setProfile({ ...profile, role: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1.5">
              Organization
            </label>
            <input
              type="text"
              value={profile.company}
              onChange={(e) => setProfile({ ...profile, company: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>
        </div>
      </Card>

      {/* Heuristic Thresholds */}
      <Card
        title="AI Diagnostic & Heuristic Thresholds"
        subtitle="Tune detection sensitivity for blurriness, oversized files, and staleness"
      >
        <div className="space-y-4 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Blur Threshold ({scannerConfig.blurThresholdPercent}%)
              </label>
              <input
                type="range"
                min="50"
                max="95"
                value={scannerConfig.blurThresholdPercent}
                onChange={(e) =>
                  setScannerConfig({
                    ...scannerConfig,
                    blurThresholdPercent: parseInt(e.target.value, 10),
                  })
                }
                className="w-full accent-indigo-600"
              />
              <p className="text-[11px] text-slate-400 mt-1">
                Images below this clarity score will be flagged as blurry.
              </p>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Large File Cutoff (MB)
              </label>
              <input
                type="number"
                value={scannerConfig.minLargeFileSizeMB}
                onChange={(e) =>
                  setScannerConfig({
                    ...scannerConfig,
                    minLargeFileSizeMB: parseInt(e.target.value, 10) || 500,
                  })
                }
                className="w-full px-3 py-2 border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
              </input>
              <p className="text-[11px] text-slate-400 mt-1">
                Files exceeding this size are flagged as large.
              </p>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Inactive Age (Days)
              </label>
              <input
                type="number"
                value={scannerConfig.inactiveDaysThreshold}
                onChange={(e) =>
                  setScannerConfig({
                    ...scannerConfig,
                    inactiveDaysThreshold: parseInt(e.target.value, 10) || 180,
                  })
                }
                className="w-full px-3 py-2 border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
              </input>
              <p className="text-[11px] text-slate-400 mt-1">
                Files unaccessed past this age are classified as stale.
              </p>
            </div>
          </div>
        </div>
      </Card>

      {/* Notifications */}
      <Card
        title="Notifications & Storage Alerts"
        subtitle="Manage email digests and capacity thresholds"
      >
        <div className="space-y-3 text-xs">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={notifications.emailWeeklyReport}
              onChange={(e) =>
                setNotifications({
                  ...notifications,
                  emailWeeklyReport: e.target.checked,
                })
              }
              className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
            />
            <span className="font-semibold text-slate-800">
              Email weekly optimization report digest
            </span>
          </label>

          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={notifications.alertOnStorageThreshold}
              onChange={(e) =>
                setNotifications({
                  ...notifications,
                  alertOnStorageThreshold: e.target.checked,
                })
              }
              className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
            />
            <span className="font-semibold text-slate-800">
              Send critical alert when storage pool exceeds 80% capacity
            </span>
          </label>

          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={notifications.notifyOnScanComplete}
              onChange={(e) =>
                setNotifications({
                  ...notifications,
                  notifyOnScanComplete: e.target.checked,
                })
              }
              className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
            />
            <span className="font-semibold text-slate-800">
              Show desktop notifications when background scans finish
            </span>
          </label>
        </div>
      </Card>
    </div>
  );
};

export default UserSettings;

