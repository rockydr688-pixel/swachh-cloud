import React, { useState } from 'react';
import PageHeader from '../components/layout/PageHeader';
import StorageCard from '../components/storage/StorageCard';
import ConnectModal from '../components/storage/ConnectModal';
import { useStorage } from '../context/StorageContext';
import LoadingSpinner from '../components/common/LoadingSpinner';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import {
  HardDrive,
  Cloud,
  Plus,
  ShieldCheck,
  Server,
  Layers,
  Box,
  Terminal,
  Info,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

export const StorageConnections = () => {
  const { connections, loading, runScan, refreshData } = useStorage();
  const [connectModalOpen, setConnectModalOpen] = useState(false);
  const [selectedProviderType, setSelectedProviderType] = useState('local');

  if (loading) {
    return <LoadingSpinner fullPage message="Querying storage agents & cloud providers..." />;
  }

  const openConnectFor = (providerType) => {
    setSelectedProviderType(providerType);
    setConnectModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Storage Connections"
        subtitle="Manage actual local storage directories and enterprise cloud accounts (Google Drive, OneDrive, Dropbox)."
        breadcrumbs={[{ label: 'Storage Connections' }]}
        actions={
          <div className="flex items-center gap-3">
            <Badge variant="navy" size="md">
              {connections.length} Storage Targets
            </Badge>
            <Button
              variant="primary"
              size="sm"
              icon={Plus}
              onClick={() => openConnectFor('google_drive')}
              className="bg-indigo-600 hover:bg-indigo-700"
            >
              Connect Cloud Account
            </Button>
          </div>
        }
      />

      {/* Cloud Integration Architecture Banner */}
      <div className="p-4 rounded-xl bg-slate-900 text-slate-300 border border-slate-800 flex items-start gap-3.5 shadow-sm">
        <div className="p-2 rounded-lg bg-indigo-500/20 text-indigo-400 shrink-0 mt-0.5">
          <Server className="w-5 h-5" />
        </div>
        <div className="space-y-1 text-xs">
          <h4 className="font-bold text-white text-sm">
            Universal Multi-Cloud Provider Abstraction
          </h4>
          <p className="text-slate-400 leading-relaxed">
            Connect actual accounts across <strong>Google Drive</strong>, <strong>Microsoft OneDrive</strong>, and <strong>Dropbox</strong> using official OAuth 2.0 with AES-256 encrypted refresh tokens. Local disks are scanned via the background agent daemon with zero external byte transmission.
          </p>
        </div>
      </div>

      {/* Storage Connection Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-4 gap-6">
        {connections.map((conn) => (
          <StorageCard
            key={conn.id}
            connection={conn}
            onTriggerScan={(targetName, storageType, connId) =>
              runScan({
                target: targetName,
                storageType: storageType || conn.type,
                storageConnectionId: connId || conn.id,
              })
            }
          />
        ))}
      </div>

      {/* Quick Add Cloud Account Bar */}
      <Card
        title="Connect Additional Storage Accounts"
        subtitle="CloudClean AI supports linking multiple accounts simultaneously across all supported providers"
      >
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 pt-1">
          <button
            type="button"
            onClick={() => openConnectFor('local')}
            className="p-3.5 rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/40 transition-all flex items-center gap-3 text-left group"
          >
            <div className="p-2 rounded-lg bg-blue-100 text-blue-700 group-hover:scale-105 transition-transform">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-slate-900 text-xs block">Local Computer</span>
              <span className="text-[11px] text-slate-500">Add folder paths</span>
            </div>
          </button>

          <button
            type="button"
            onClick={() => openConnectFor('google_drive')}
            className="p-3.5 rounded-xl border border-slate-200 hover:border-amber-400 hover:bg-amber-50/40 transition-all flex items-center gap-3 text-left group"
          >
            <div className="p-2 rounded-lg bg-amber-100 text-amber-700 group-hover:scale-105 transition-transform">
              <Cloud className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-slate-900 text-xs block">Google Drive</span>
              <span className="text-[11px] text-slate-500">Link Google Workspace</span>
            </div>
          </button>

          <button
            type="button"
            onClick={() => openConnectFor('onedrive')}
            className="p-3.5 rounded-xl border border-slate-200 hover:border-sky-400 hover:bg-sky-50/40 transition-all flex items-center gap-3 text-left group"
          >
            <div className="p-2 rounded-lg bg-sky-100 text-sky-700 group-hover:scale-105 transition-transform">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-slate-900 text-xs block">Microsoft OneDrive</span>
              <span className="text-[11px] text-slate-500">Link Microsoft 365</span>
            </div>
          </button>

          <button
            type="button"
            onClick={() => openConnectFor('dropbox')}
            className="p-3.5 rounded-xl border border-slate-200 hover:border-indigo-400 hover:bg-indigo-50/40 transition-all flex items-center gap-3 text-left group"
          >
            <div className="p-2 rounded-lg bg-indigo-100 text-indigo-700 group-hover:scale-105 transition-transform">
              <Box className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-slate-900 text-xs block">Dropbox</span>
              <span className="text-[11px] text-slate-500">Link Dropbox storage</span>
            </div>
          </button>
        </div>
      </Card>

      {/* Technical Integration Reference Card */}
      <Card
        title="Local Agent Daemon & Security Protocol"
        subtitle="Run the CloudClean local CLI agent on Windows, macOS, or Linux"
      >
        <div className="space-y-4 text-xs">
          <p className="text-slate-600">
            The local agent runs as a low-overhead background service, reading file systems, computing partial MD5/SHA-256 hashes for duplicate identification, and extracting image sharpness scores without transferring files.
          </p>

          <div className="p-3.5 rounded-lg bg-slate-900 text-slate-200 font-mono text-xs overflow-x-auto space-y-1">
            <p className="text-slate-400"># Start local agent daemon in background</p>
            <p className="text-indigo-400">$ cloudclean-agent start --port 9876 --watch "C:/Users/rocky/Downloads"</p>
            <p className="text-emerald-400">✓ Local agent listening on ws://127.0.0.1:9876/ipc [Connected]</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
            <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/60">
              <span className="font-bold text-slate-800 block">Zero Cloud Upload</span>
              <span className="text-slate-500 text-[11px]">
                File contents stay 100% on your machine.
              </span>
            </div>
            <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/60">
              <span className="font-bold text-slate-800 block">Fast Hashing</span>
              <span className="text-slate-500 text-[11px]">
                Multi-threaded I/O with early header exit.
              </span>
            </div>
            <div className="p-3 rounded-lg border border-slate-100 bg-slate-50/60">
              <span className="font-bold text-slate-800 block">Trash Safety Net</span>
              <span className="text-slate-500 text-[11px]">
                Files are moved to system recycle bin by default.
              </span>
            </div>
          </div>
        </div>
      </Card>

      <ConnectModal
        isOpen={connectModalOpen}
        onClose={() => setConnectModalOpen(false)}
        type={selectedProviderType}
        onSuccess={refreshData}
      />
    </div>
  );
};

export default StorageConnections;
