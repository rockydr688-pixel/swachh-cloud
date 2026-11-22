import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useStorage } from '../context/StorageContext';
import { mockUserSettings } from '../data/mockData';
import Button from '../components/common/Button';
import Card from '../components/common/Card';
import Badge from '../components/common/Badge';
import {
  User,
  HardDrive,
  Sliders,
  Clock,
  Image,
  Shield,
  Bell,
  Lock,
  LogOut,
  Save,
  CheckCircle2,
  Trash2,
  ChevronRight,
  ExternalLink,
} from 'lucide-react';

export const UserSettings = () => {
  const navigate = useNavigate();
  const { user, updateProfile, logout } = useAuth();
  const { connections = [], disconnectStorage } = useStorage();

  const [activeSection, setActiveSection] = useState('profile');
  const [profile, setProfile] = useState({
    name: user?.name || mockUserSettings.profile.fullName,
    email: user?.email || mockUserSettings.profile.email,
  });

  const [scanPrefs, setScanPrefs] = useState({
    detectDuplicates: true,
    detectBlurry: true,
    detectLarge: true,
    detectInactive: true,
    detectTemp: true,
  });

  const [inactivityDays, setInactivityDays] = useState(180);
  const [blurSensitivity, setBlurSensitivity] = useState('normal'); // 'low' | 'normal' | 'high'
  const [largeFileMb, setLargeFileMb] = useState(500);

  const [protectedPaths, setProtectedPaths] = useState([
    '*.docx',
    '*.xlsx',
    'FamilyPhotos/*',
    'Contracts/*',
  ]);
  const [newPathInput, setNewPathInput] = useState('');

  const [notifications, setNotifications] = useState({
    emailDigest: true,
    capacityAlerts: true,
    scanCompleted: true,
  });

  const [savedMessage, setSavedMessage] = useState(null);

  const handleSave = (e) => {
    e?.preventDefault();
    if (updateProfile) {
      updateProfile({ name: profile.name, email: profile.email });
    }
    setSavedMessage('Settings successfully saved.');
    setTimeout(() => setSavedMessage(null), 4000);
  };

  const handleAddProtectedPath = (e) => {
    e.preventDefault();
    if (!newPathInput.trim()) return;
    setProtectedPaths([...protectedPaths, newPathInput.trim()]);
    setNewPathInput('');
  };

  const handleRemoveProtectedPath = (index) => {
    setProtectedPaths(protectedPaths.filter((_, idx) => idx !== index));
  };

  const handleDisconnect = async (id, name) => {
    if (!window.confirm(`Are you sure you want to disconnect ${name}?`)) return;
    try {
      if (disconnectStorage) await disconnectStorage(id);
    } catch (e) {
      console.error(e);
    }
  };

  const sections = [
    { id: 'profile', label: 'Profile', icon: User },
    { id: 'accounts', label: 'Connected Accounts', icon: HardDrive },
    { id: 'scan', label: 'Scan Preferences', icon: Sliders },
    { id: 'inactivity', label: 'Inactivity Thresholds', icon: Clock },
    { id: 'image', label: 'Image Analysis', icon: Image },
    { id: 'protected', label: 'Protected Files & Folders', icon: Shield },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'security', label: 'Account Security', icon: Lock },
  ];

  return (
    <div className="space-y-6 font-sans max-w-5xl">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
              Preferences
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Settings
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage your account, connected cloud storage, and junk file detection rules.
          </p>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <Button
            variant="primary"
            size="md"
            icon={Save}
            onClick={handleSave}
            className="bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-sm"
          >
            Save Changes
          </Button>
        </div>
      </div>

      {/* Success Notification */}
      {savedMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 flex items-center gap-2 font-semibold animate-fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>{savedMessage}</span>
        </div>
      )}

      {/* Main Settings Grid: Navigation Sidebar & Content */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 items-start">
        {/* Navigation Tabs */}
        <div className="bg-white rounded-2xl border border-slate-200/80 p-2 shadow-sm space-y-1">
          {sections.map((sec) => {
            const Icon = sec.icon;
            const isActive = activeSection === sec.id;
            return (
              <button
                key={sec.id}
                onClick={() => setActiveSection(sec.id)}
                className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all text-left ${
                  isActive
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span className="truncate">{sec.label}</span>
              </button>
            );
          })}
        </div>

        {/* Content Area */}
        <div className="md:col-span-3 bg-white rounded-2xl border border-slate-200/80 p-6 shadow-sm space-y-6">
          {/* 1. Profile Section */}
          {activeSection === 'profile' && (
            <div className="space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">User Profile</h3>
                <p className="text-xs text-slate-500">Your CloudClean AI account details</p>
              </div>

              <div className="space-y-4 max-w-md">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Full Name
                  </label>
                  <input
                    type="text"
                    value={profile.name}
                    onChange={(e) => setProfile({ ...profile, name: e.target.value })}
                    className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Email Address
                  </label>
                  <input
                    type="email"
                    value={profile.email}
                    onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                    className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                </div>
              </div>
            </div>
          )}

          {/* 2. Connected Accounts */}
          {activeSection === 'accounts' && (
            <div className="space-y-5">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-slate-900">Connected Accounts</h3>
                  <p className="text-xs text-slate-500">Manage cloud accounts linked to CloudClean AI</p>
                </div>
                <Button
                  size="xs"
                  variant="outline"
                  onClick={() => navigate('/storage')}
                  className="text-blue-600 border-blue-200"
                >
                  Manage in My Storage
                </Button>
              </div>

              <div className="divide-y divide-slate-100">
                {connections.map((conn) => (
                  <div key={conn.id} className="py-3 flex items-center justify-between gap-4">
                    <div className="min-w-0">
                      <div className="text-sm font-bold text-slate-900 truncate">{conn.name}</div>
                      <div className="text-xs text-slate-400 capitalize">{conn.type?.replace('_', ' ')} · {conn.status}</div>
                    </div>
                    {conn.type !== 'local' && conn.status === 'connected' && (
                      <button
                        onClick={() => handleDisconnect(conn.id, conn.name)}
                        className="text-xs text-rose-600 hover:text-rose-700 font-semibold"
                      >
                        Disconnect Account
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 3. Scan Preferences */}
          {activeSection === 'scan' && (
            <div className="space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">Scan Preferences</h3>
                <p className="text-xs text-slate-500">Choose which categories of files should be detected</p>
              </div>

              <div className="space-y-3">
                {[
                  { key: 'detectDuplicates', label: 'Exact Duplicate Detection', desc: 'Identifies identical duplicate files using cryptographic hash checks.' },
                  { key: 'detectBlurry', label: 'Blurry Photo Diagnostics', desc: 'Analyzes camera photos for motion blur and low sharpness.' },
                  { key: 'detectLarge', label: 'Large File Audits', desc: 'Finds heavy videos, disk images, and archives consuming large amounts of space.' },
                  { key: 'detectInactive', label: 'Old Stale Files', desc: 'Finds downloads and documents not opened in a long time.' },
                  { key: 'detectTemp', label: 'Temporary Files', desc: 'Flags crash logs, temp caches, and installation residue.' },
                ].map((item) => (
                  <label key={item.key} className="flex items-start gap-3 p-3 rounded-xl border border-slate-100 hover:bg-slate-50 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={scanPrefs[item.key]}
                      onChange={(e) => setScanPrefs({ ...scanPrefs, [item.key]: e.target.checked })}
                      className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4 mt-0.5 cursor-pointer"
                    />
                    <div>
                      <div className="text-xs font-bold text-slate-800">{item.label}</div>
                      <div className="text-[11px] text-slate-500">{item.desc}</div>
                    </div>
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* 4. Inactivity Thresholds */}
          {activeSection === 'inactivity' && (
            <div className="space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">Inactivity Thresholds</h3>
                <p className="text-xs text-slate-500">Define when a file should be classified as old or inactive</p>
              </div>

              <div className="space-y-3 max-w-md">
                <label className="block text-xs font-semibold text-slate-700">
                  Classify as old if not opened for:
                </label>
                <select
                  value={inactivityDays}
                  onChange={(e) => setInactivityDays(Number(e.target.value))}
                  className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl text-slate-800 focus:outline-none"
                >
                  <option value={30}>30 Days (1 Month)</option>
                  <option value={90}>90 Days (3 Months)</option>
                  <option value={180}>180 Days (6 Months - Recommended)</option>
                  <option value={365}>365 Days (1 Year)</option>
                  <option value={730}>730 Days (2 Years)</option>
                </select>

                <div className="pt-2">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Large File Threshold (MB):
                  </label>
                  <input
                    type="number"
                    value={largeFileMb}
                    onChange={(e) => setLargeFileMb(Number(e.target.value))}
                    className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl text-slate-800 focus:outline-none"
                  />
                  <p className="text-[11px] text-slate-400 mt-1">Files larger than this will be flagged as heavy storage consumers.</p>
                </div>
              </div>
            </div>
          )}

          {/* 5. Image Analysis Preferences */}
          {activeSection === 'image' && (
            <div className="space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">Image Analysis Preferences</h3>
                <p className="text-xs text-slate-500">Configure sensitivity for blur and photo quality evaluation</p>
              </div>

              <div className="space-y-2 max-w-md">
                {[
                  { id: 'low', label: 'Conservative', desc: 'Only flag heavily blurred or unrecognizable shots.' },
                  { id: 'normal', label: 'Normal (Recommended)', desc: 'Balanced detection of out-of-focus and blurry burst photos.' },
                  { id: 'high', label: 'Strict', desc: 'Flags subtle softness and slight lens blur.' },
                ].map((sens) => (
                  <label
                    key={sens.id}
                    className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-colors ${
                      blurSensitivity === sens.id
                        ? 'border-blue-500 bg-blue-50/20'
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <input
                      type="radio"
                      name="blurSensitivity"
                      value={sens.id}
                      checked={blurSensitivity === sens.id}
                      onChange={(e) => setBlurSensitivity(e.target.value)}
                      className="text-blue-600 focus:ring-blue-500 mt-0.5"
                    />
                    <div>
                      <div className="text-xs font-bold text-slate-800">{sens.label}</div>
                      <div className="text-[11px] text-slate-500">{sens.desc}</div>
                    </div>
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* 6. Protected Files & Folders */}
          {activeSection === 'protected' && (
            <div className="space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">Protected Files & Folders</h3>
                <p className="text-xs text-slate-500">Files matching these patterns will never be suggested for deletion</p>
              </div>

              {/* Add form */}
              <form onSubmit={handleAddProtectedPath} className="flex gap-2 max-w-md">
                <input
                  type="text"
                  placeholder="e.g. *.pdf, Tax2024/*, Receipts/*"
                  value={newPathInput}
                  onChange={(e) => setNewPathInput(e.target.value)}
                  className="flex-1 px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl text-slate-800 focus:outline-none"
                />
                <Button size="sm" variant="primary" type="submit" className="bg-blue-600 hover:bg-blue-700 text-white font-medium">
                  Add Rule
                </Button>
              </form>

              {/* List */}
              <div className="space-y-2 max-w-md">
                {protectedPaths.map((rule, idx) => (
                  <div key={idx} className="flex items-center justify-between p-2.5 bg-slate-50 rounded-xl border border-slate-200 text-xs">
                    <span className="font-mono text-slate-700 font-semibold">{rule}</span>
                    <button
                      onClick={() => handleRemoveProtectedPath(idx)}
                      className="text-slate-400 hover:text-rose-600 p-1 rounded"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 7. Notification Preferences */}
          {activeSection === 'notifications' && (
            <div className="space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">Notification Preferences</h3>
                <p className="text-xs text-slate-500">Configure how and when you receive cleaning notifications</p>
              </div>

              <div className="space-y-3">
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={notifications.emailDigest}
                    onChange={(e) => setNotifications({ ...notifications, emailDigest: e.target.checked })}
                    className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4"
                  />
                  <div>
                    <div className="text-xs font-semibold text-slate-800">Weekly Storage Summary Digest</div>
                    <div className="text-[11px] text-slate-500">Receive an email showing storage trends and potential savings.</div>
                  </div>
                </label>

                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={notifications.capacityAlerts}
                    onChange={(e) => setNotifications({ ...notifications, capacityAlerts: e.target.checked })}
                    className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4"
                  />
                  <div>
                    <div className="text-xs font-semibold text-slate-800">Critical Storage Alerts</div>
                    <div className="text-[11px] text-slate-500">Notify immediately when any connected drive exceeds 85% capacity.</div>
                  </div>
                </label>

                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={notifications.scanCompleted}
                    onChange={(e) => setNotifications({ ...notifications, scanCompleted: e.target.checked })}
                    className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4"
                  />
                  <div>
                    <div className="text-xs font-semibold text-slate-800">Scan Finished Notifications</div>
                    <div className="text-[11px] text-slate-500">Show notification in-app when a background scan completes.</div>
                  </div>
                </label>
              </div>
            </div>
          )}

          {/* 8. Account Security & Disconnect */}
          {activeSection === 'security' && (
            <div className="space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">Account Security</h3>
                <p className="text-xs text-slate-500">Manage security credentials and session authentication</p>
              </div>

              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs space-y-3 max-w-md">
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Password:</span>
                  <span className="font-mono text-slate-800">••••••••••••</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Session Status:</span>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-bold text-[10px]">
                    Active JWT Session
                  </span>
                </div>
              </div>

              <div className="pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  icon={LogOut}
                  onClick={() => {
                    logout();
                    navigate('/auth');
                  }}
                  className="text-rose-600 border-rose-200 hover:bg-rose-50 text-xs font-semibold"
                >
                  Sign Out from CloudClean AI
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserSettings;
