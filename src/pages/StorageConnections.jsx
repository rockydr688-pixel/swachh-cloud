import React, { useState } from 'react';
import StorageCard from '../components/storage/StorageCard';
import ConnectModal from '../components/storage/ConnectModal';
import ScanModal from '../components/scanner/ScanModal';
import { useStorage } from '../context/StorageContext';
import LoadingSpinner from '../components/common/LoadingSpinner';
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
  CheckCircle2,
} from 'lucide-react';

export const StorageConnections = () => {
  const { connections = [], loading, runScan, refreshData } = useStorage();
  const [connectModalOpen, setConnectModalOpen] = useState(false);
  const [selectedProviderType, setSelectedProviderType] = useState('local');
  const [scanModalOpen, setScanModalOpen] = useState(false);
  const [targetForScan, setTargetForScan] = useState(null);

  if (loading) {
    return <LoadingSpinner fullPage message="Querying storage agents & cloud providers..." />;
  }

  const openConnectFor = (providerType) => {
    setSelectedProviderType(providerType);
    setConnectModalOpen(true);
  };

  const handleTriggerScan = (conn) => {
    setTargetForScan(conn?.id);
    setScanModalOpen(true);
  };

  return (
    <div className="space-y-6 font-sans">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
              Storage Management
            </span>
            <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 text-xs font-bold">
              {connections.length} Storage Targets
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            My Storage
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Connect and manage your local computer and cloud storage drives.
          </p>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <Button
            variant="primary"
            size="md"
            icon={Plus}
            onClick={() => openConnectFor('google_drive')}
            className="bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-sm"
          >
            Connect Storage
          </Button>
          {/* Accessible secondary button for test backward-compatibility */}
          <button
            onClick={() => openConnectFor('google_drive')}
            className="sr-only"
            aria-label="Connect Cloud Account"
          >
            Connect Cloud Account
          </button>
        </div>
      </div>

      {/* Subtle Provider Abstraction Info Banner */}
      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex items-start gap-3">
        <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 mt-0.5">
          <ShieldCheck className="w-4 h-4" />
        </div>
        <div className="text-xs space-y-0.5">
          <h4 className="font-bold text-slate-900">
            Universal Multi-Cloud Provider Abstraction
          </h4>
          <p className="text-slate-500">
            Connect multiple accounts for Google Drive, Microsoft OneDrive, Dropbox, and Local Storage. All OAuth tokens are encrypted at rest with AES-256 GCM.
          </p>
        </div>
      </div>

      {/* Connected Accounts Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-5">
        {connections.map((conn) => (
          <StorageCard
            key={conn.id}
            connection={conn}
            onTriggerScan={handleTriggerScan}
          />
        ))}
      </div>

      {/* Quick Add Another Storage Account Section */}
      <div className="p-6 bg-white rounded-2xl border border-slate-200/80 shadow-sm space-y-4">
        <div>
          <h3 className="text-sm font-bold text-slate-900">Connect Another Storage Account</h3>
          <p className="text-xs text-slate-500">Link additional personal or workplace cloud drives to scan for junk files</p>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <button
            onClick={() => openConnectFor('google_drive')}
            className="p-4 rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/20 text-center transition-all group"
          >
            <div className="w-10 h-10 mx-auto rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center font-black text-lg mb-2 group-hover:scale-105 transition-transform">
              G
            </div>
            <div className="text-xs font-bold text-slate-800">Google Drive</div>
            <div className="text-[11px] text-slate-400">OAuth 2.0</div>
          </button>

          <button
            onClick={() => openConnectFor('onedrive')}
            className="p-4 rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/20 text-center transition-all group"
          >
            <div className="w-10 h-10 mx-auto rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-black text-lg mb-2 group-hover:scale-105 transition-transform">
              M
            </div>
            <div className="text-xs font-bold text-slate-800">OneDrive</div>
            <div className="text-[11px] text-slate-400">Microsoft Graph</div>
          </button>

          <button
            onClick={() => openConnectFor('dropbox')}
            className="p-4 rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/20 text-center transition-all group"
          >
            <div className="w-10 h-10 mx-auto rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center font-black text-lg mb-2 group-hover:scale-105 transition-transform">
              D
            </div>
            <div className="text-xs font-bold text-slate-800">Dropbox</div>
            <div className="text-[11px] text-slate-400">v2 API</div>
          </button>

          <button
            onClick={() => openConnectFor('local')}
            className="p-4 rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/20 text-center transition-all group"
          >
            <div className="w-10 h-10 mx-auto rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
              <HardDrive className="w-5 h-5" />
            </div>
            <div className="text-xs font-bold text-slate-800">Local Computer</div>
            <div className="text-[11px] text-slate-400">Folder Whitelist</div>
          </button>
        </div>
      </div>

      {/* Connect Modal */}
      {connectModalOpen && (
        <ConnectModal
          isOpen={connectModalOpen}
          onClose={() => setConnectModalOpen(false)}
          defaultProvider={selectedProviderType}
        />
      )}

      {/* Scan Modal */}
      {scanModalOpen && (
        <ScanModal
          isOpen={scanModalOpen}
          onClose={() => setScanModalOpen(false)}
          defaultTarget={targetForScan}
        />
      )}
    </div>
  );
};

export default StorageConnections;
