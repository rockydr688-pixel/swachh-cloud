import React, { useState } from 'react';
import PageHeader from '../components/layout/PageHeader';
import StorageCard from '../components/storage/StorageCard';
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
  Terminal,
  Info,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

export const StorageConnections = () => {
  const { connections, loading, runScan } = useStorage();

  if (loading) {
    return <LoadingSpinner fullPage message="Querying storage agents & cloud providers..." />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Storage Connections"
        subtitle="Configure physical local storage folders via daemon IPC and enterprise cloud repositories."
        breadcrumbs={[{ label: 'Storage Connections' }]}
        actions={
          <Badge variant="navy" size="md">
            2 Storage Adapters
          </Badge>
        }
      />

      {/* Cloud Integration Disclaimer */}
      <div className="p-4 rounded-xl bg-slate-900 text-slate-300 border border-slate-800 flex items-start gap-3.5 shadow-sm">
        <div className="p-2 rounded-lg bg-indigo-500/20 text-indigo-400 shrink-0 mt-0.5">
          <Server className="w-5 h-5" />
        </div>
        <div className="space-y-1 text-xs">
          <h4 className="font-bold text-white text-sm">
            Architecture & Connection Security Standard
          </h4>
          <p className="text-slate-400 leading-relaxed">
            Local drives are indexed with zero byte exfiltration via the native client agent daemon. Cloud connectors require backend OAuth 2.0 PKCE authentication flow. In this frontend stage, Google Drive strictly reflects the non-authenticated pending state.
          </p>
        </div>
      </div>

      {/* Two Storage Connection Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {connections.map((conn) => (
          <StorageCard
            key={conn.id}
            connection={conn}
            onTriggerScan={(targetName) => runScan(targetName)}
          />
        ))}
      </div>

      {/* Technical Integration Reference Card */}
      <Card
        title="Local Agent Daemon Status & Quick Start"
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
    </div>
  );
};

export default StorageConnections;

