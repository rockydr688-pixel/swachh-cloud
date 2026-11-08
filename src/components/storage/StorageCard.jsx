import React, { useState } from 'react';
import Card from '../common/Card';
import Badge from '../common/Badge';
import Button from '../common/Button';
import ProgressBar from '../common/ProgressBar';
import { formatBytes, formatDateTime } from '../../utils/formatters';
import {
  HardDrive,
  Cloud,
  FolderTree,
  Clock,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Lock,
  Plus,
  Trash2,
  Play,
  ShieldAlert,
} from 'lucide-react';
import ConnectModal from './ConnectModal';

export const StorageCard = ({ connection, onTriggerScan }) => {
  const [modalOpen, setModalOpen] = useState(false);

  const isLocal = connection.type === 'local';
  const isConnected = connection.status === 'connected';

  return (
    <>
      <Card
        className="flex flex-col justify-between h-full relative overflow-hidden"
        hoverEffect
      >
        <div>
          {/* Card Header */}
          <div className="flex items-start justify-between gap-4 mb-4">
            <div className="flex items-center gap-3">
              <div
                className={`p-3 rounded-xl border ${
                  isLocal
                    ? 'bg-blue-50 text-blue-600 border-blue-100'
                    : 'bg-violet-50 text-violet-600 border-violet-100'
                }`}
              >
                {isLocal ? <HardDrive className="w-6 h-6" /> : <Cloud className="w-6 h-6" />}
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900 tracking-tight">
                  {connection.name}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  {isLocal ? 'Local Daemon & Agent IPC' : 'Google Workspace Cloud Drive'}
                </p>
              </div>
            </div>

            {/* Connection Status Badge */}
            {isConnected ? (
              <Badge variant="success" size="sm" dot>
                Connected
              </Badge>
            ) : (
              <Badge variant="warning" size="sm" dot>
                Disconnected
              </Badge>
            )}
          </div>

          {/* Description */}
          <p className="text-xs text-slate-600 leading-relaxed mb-4">
            {connection.description}
          </p>

          {/* Storage Capacity / Usage Meter if connected */}
          {isConnected && connection.totalCapacityBytes > 0 && (
            <div className="p-3 bg-slate-50/80 rounded-xl border border-slate-100 mb-4 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Storage Allocated</span>
                <span className="font-bold text-slate-800">
                  {formatBytes(connection.totalUsedBytes)} / {formatBytes(connection.totalCapacityBytes)}
                </span>
              </div>
              <ProgressBar
                value={connection.totalUsedBytes}
                max={connection.totalCapacityBytes}
                color={isLocal ? 'blue' : 'violet'}
                size="sm"
              />
            </div>
          )}

          {/* Details / Metadata */}
          <div className="space-y-2.5 text-xs border-t border-slate-100 pt-3">
            {/* Last Scan Time */}
            <div className="flex items-center justify-between">
              <span className="text-slate-400 font-medium flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400" />
                Last Scan Time:
              </span>
              <span className="font-semibold text-slate-700">
                {connection.lastScanTime ? formatDateTime(connection.lastScanTime) : 'Never Scanned'}
              </span>
            </div>

            {/* Local Specific: Monitored Folders */}
            {isLocal && (
              <div className="space-y-1.5 pt-1">
                <span className="text-slate-400 font-medium flex items-center gap-1.5">
                  <FolderTree className="w-3.5 h-3.5 text-slate-400" />
                  Monitored Paths ({connection.folders?.length || 0}):
                </span>
                <div className="space-y-1">
                  {connection.folders?.map((f, i) => (
                    <div
                      key={i}
                      className="px-2.5 py-1.5 bg-slate-50 rounded-lg border border-slate-200/60 font-mono text-[11px] text-slate-600 flex items-center justify-between"
                    >
                      <span className="truncate max-w-[200px]" title={f.path}>
                        {f.path}
                      </span>
                      <span className="font-sans text-[10px] text-slate-400 font-semibold shrink-0">
                        {formatBytes(f.sizeBytes)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Google Drive Specific: OAuth State and Connected Account */}
            {!isLocal && (
              <div className="space-y-2 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-medium flex items-center gap-1.5">
                    <Lock className="w-3.5 h-3.5 text-slate-400" />
                    OAuth Status:
                  </span>
                  <span className="font-semibold text-amber-600 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" />
                    Not Authenticated
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-medium">Connected Account:</span>
                  <span className="font-mono text-[11px] text-slate-500">
                    {connection.accountEmail || 'None (Requires OAuth)'}
                  </span>
                </div>

                <div className="p-2.5 bg-amber-50/80 rounded-lg border border-amber-200/70 text-[11px] text-amber-800 leading-tight">
                  OAuth 2.0 flow is gated pending real backend service integration. Dummy cloud credentials are strictly not simulated.
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Card Footer Actions */}
        <div className="border-t border-slate-100 pt-4 mt-6 flex items-center justify-between gap-3">
          {isLocal ? (
            <>
              <Button
                variant="secondary"
                size="sm"
                icon={Plus}
                onClick={() => setModalOpen(true)}
              >
                Configure Folders
              </Button>
              <Button
                variant="primary"
                size="sm"
                icon={Play}
                onClick={() => onTriggerScan && onTriggerScan(connection.name)}
                className="bg-indigo-600 hover:bg-indigo-700"
              >
                Scan Now
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="subtleViolet"
                size="sm"
                icon={Lock}
                onClick={() => setModalOpen(true)}
                className="w-full justify-center"
              >
                Connect Google Drive (OAuth)
              </Button>
            </>
          )}
        </div>
      </Card>

      <ConnectModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        type={connection.type}
      />
    </>
  );
};

export default StorageCard;

