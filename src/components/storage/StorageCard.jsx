import React, { useState } from 'react';
import Card from '../common/Card';
import Badge from '../common/Badge';
import Button from '../common/Button';
import ProgressBar from '../common/ProgressBar';
import { formatBytes, formatDateTime } from '../../utils/formatters';
import { useStorage } from '../../context/StorageContext';
import { useToast } from '../../context/ToastContext';
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
  ShieldCheck,
  Unlink,
  Layers,
  Box,
} from 'lucide-react';
import ConnectModal from './ConnectModal';

export const StorageCard = ({ connection, onTriggerScan }) => {
  const [modalOpen, setModalOpen] = useState(false);
  const { disconnectStorage, refreshData } = useStorage();
  const { addToast } = useToast();

  const type = connection.type ? connection.type.toLowerCase() : 'local';
  const isLocal = type === 'local';
  const isGoogle = type === 'google_drive';
  const isOneDrive = type === 'onedrive';
  const isDropbox = type === 'dropbox';
  const isConnected = connection.status === 'connected';

  const usedBytes = connection.usedBytes ?? connection.totalUsedBytes ?? 0;
  const capacityBytes = connection.totalCapacityBytes ?? 1099511627776;
  const availableBytes = Math.max(0, capacityBytes - usedBytes);

  const paths = connection.monitoredPaths || (connection.folders?.map(f => f.path)) || [];

  const handleDisconnect = async () => {
    try {
      await disconnectStorage(connection.id);
      addToast({
        title: 'Storage Disconnected',
        message: `${connection.name} has been disconnected.`,
        type: 'info',
      });
      refreshData();
    } catch (e) {
      addToast({
        title: 'Error',
        message: 'Could not disconnect storage',
        type: 'error',
      });
    }
  };

  const getProviderIcon = () => {
    if (isLocal) return <HardDrive className="w-6 h-6" />;
    if (isGoogle) return <Cloud className="w-6 h-6" />;
    if (isOneDrive) return <Layers className="w-6 h-6" />;
    if (isDropbox) return <Box className="w-6 h-6" />;
    return <Cloud className="w-6 h-6" />;
  };

  const getProviderTheme = () => {
    if (isLocal) return { bg: 'bg-blue-50 text-blue-600 border-blue-100', bar: 'blue', label: 'Local Daemon & Agent IPC' };
    if (isGoogle) return { bg: 'bg-amber-50 text-amber-600 border-amber-100', bar: 'amber', label: 'Google Workspace Cloud Drive' };
    if (isOneDrive) return { bg: 'bg-sky-50 text-sky-600 border-sky-100', bar: 'blue', label: 'Microsoft OneDrive Cloud Storage' };
    if (isDropbox) return { bg: 'bg-indigo-50 text-indigo-600 border-indigo-100', bar: 'indigo', label: 'Dropbox Cloud Repository' };
    return { bg: 'bg-violet-50 text-violet-600 border-violet-100', bar: 'violet', label: 'Cloud Storage' };
  };

  const theme = getProviderTheme();

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
              <div className={`p-3 rounded-xl border ${theme.bg}`}>
                {getProviderIcon()}
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900 tracking-tight">
                  {connection.name}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  {theme.label}
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
            {connection.description || 'Cloud repository integrated with automated junk detection and trash retention.'}
          </p>

          {/* Storage Capacity / Usage Meter */}
          {isConnected && capacityBytes > 0 && (
            <div className="p-3 bg-slate-50/80 rounded-xl border border-slate-100 mb-4 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Storage Allocated</span>
                <span className="font-bold text-slate-800">
                  {formatBytes(usedBytes)} / {formatBytes(capacityBytes)}
                </span>
              </div>
              <ProgressBar
                value={usedBytes}
                max={capacityBytes}
                color={theme.bar}
                size="sm"
              />
              <div className="flex items-center justify-between text-[11px] text-slate-400 pt-0.5">
                <span>Free: {formatBytes(availableBytes)}</span>
                <span>{capacityBytes > 0 ? ((usedBytes / capacityBytes) * 100).toFixed(1) : 0}% used</span>
              </div>
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
                  Monitored Paths ({paths.length}):
                </span>
                <div className="space-y-1">
                  {paths.map((p, i) => (
                    <div
                      key={i}
                      className="px-2.5 py-1.5 bg-slate-50 rounded-lg border border-slate-200/60 font-mono text-[11px] text-slate-600 flex items-center justify-between"
                    >
                      <span className="truncate max-w-[260px]" title={p}>
                        {p}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Cloud Provider Specific: OAuth State and Connected Account */}
            {!isLocal && (
              <div className="space-y-2 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-medium flex items-center gap-1.5">
                    <Lock className="w-3.5 h-3.5 text-slate-400" />
                    OAuth Status:
                  </span>
                  {isConnected ? (
                    <span className="font-semibold text-emerald-600 flex items-center gap-1">
                      <CheckCircle2 className="w-3 h-3" />
                      Authorized & Active
                    </span>
                  ) : (
                    <span className="font-semibold text-amber-600 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      Not Authenticated
                    </span>
                  )}
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-medium">Connected Account:</span>
                  <span className="font-mono text-[11px] text-slate-600 font-medium truncate max-w-[220px]">
                    {connection.accountEmail || (isConnected ? 'Account Active' : 'None (Requires OAuth)')}
                  </span>
                </div>

                {!isConnected && (
                  <div className="p-2.5 bg-amber-50/80 rounded-lg border border-amber-200/70 text-[11px] text-amber-800 leading-tight">
                    Connect via official OAuth 2.0 to scan actual files and retrieve real metadata.
                  </div>
                )}
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
                onClick={() => onTriggerScan && onTriggerScan(connection.name, connection.type, connection.id)}
                className="bg-indigo-600 hover:bg-indigo-700"
              >
                Scan Now
              </Button>
            </>
          ) : (
            <>
              {isConnected ? (
                <>
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={Unlink}
                    onClick={handleDisconnect}
                    className="text-rose-600 hover:bg-rose-50"
                  >
                    Disconnect
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    icon={Play}
                    onClick={() => onTriggerScan && onTriggerScan(connection.name, connection.type, connection.id)}
                    className="bg-indigo-600 hover:bg-indigo-700"
                  >
                    Scan Drive
                  </Button>
                </>
              ) : (
                <Button
                  variant="subtleViolet"
                  size="sm"
                  icon={Lock}
                  onClick={() => setModalOpen(true)}
                  className="w-full justify-center"
                >
                  Connect {connection.name} (OAuth)
                </Button>
              )}
            </>
          )}
        </div>
      </Card>

      <ConnectModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        type={connection.type}
        onSuccess={refreshData}
      />
    </>
  );
};

export default StorageCard;
