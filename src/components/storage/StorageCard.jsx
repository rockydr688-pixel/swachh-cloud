import React, { useState } from 'react';
import Badge from '../common/Badge';
import Button from '../common/Button';
import ProgressBar from '../common/ProgressBar';
import { formatBytes, formatDate } from '../../utils/formatters';
import { useStorage } from '../../context/StorageContext';
import { useToast } from '../../context/ToastContext';
import {
  HardDrive,
  Cloud,
  CheckCircle2,
  AlertCircle,
  Play,
  Unlink,
  Layers,
  Box,
  Clock,
  Sparkles,
} from 'lucide-react';
import ConnectModal from './ConnectModal';

export const StorageCard = ({ connection, onTriggerScan }) => {
  const [modalOpen, setModalOpen] = useState(false);
  const { disconnectStorage, refreshData } = useStorage();

  let addToast = () => {};
  try {
    const toast = useToast();
    if (toast && toast.addToast) addToast = toast.addToast;
  } catch (e) {}

  const type = connection.type ? connection.type.toLowerCase() : 'local';
  const isLocal = type === 'local';
  const isGoogle = type === 'google_drive';
  const isOneDrive = type === 'onedrive';
  const isDropbox = type === 'dropbox';
  const isConnected = connection.status === 'connected';

  const usedBytes = connection.usedBytes ?? connection.totalUsedBytes ?? connection.usedSpaceBytes ?? 0;
  const capacityBytes = connection.totalCapacityBytes ?? connection.totalSpaceBytes ?? 1099511627776;
  const usedPercent = Math.min(100, Math.round((usedBytes / (capacityBytes || 1)) * 100));

  const handleDisconnect = async () => {
    if (!window.confirm(`Are you sure you want to disconnect ${connection.name}?`)) return;
    try {
      if (disconnectStorage) {
        await disconnectStorage(connection.id);
      }
      addToast({
        title: 'Storage Disconnected',
        message: `${connection.name} has been disconnected.`,
        type: 'info',
      });
      if (refreshData) refreshData();
    } catch (e) {
      addToast({
        title: 'Error',
        message: 'Could not disconnect storage',
        type: 'error',
      });
    }
  };

  const getProviderBrand = () => {
    if (isLocal) {
      return {
        label: 'Local Computer',
        icon: <HardDrive className="w-5 h-5 text-slate-700" />,
        color: 'bg-slate-100 text-slate-700',
      };
    }
    if (isGoogle) {
      return {
        label: 'Google Drive',
        icon: <span className="font-bold text-amber-500 text-base">G</span>,
        color: 'bg-amber-50 text-amber-700',
      };
    }
    if (isOneDrive) {
      return {
        label: 'Microsoft OneDrive',
        icon: <Layers className="w-5 h-5 text-blue-600" />,
        color: 'bg-blue-50 text-blue-700',
      };
    }
    if (isDropbox) {
      return {
        label: 'Dropbox',
        icon: <Box className="w-5 h-5 text-sky-600" />,
        color: 'bg-sky-50 text-sky-700',
      };
    }
    return {
      label: 'Cloud Storage',
      icon: <Cloud className="w-5 h-5 text-slate-700" />,
      color: 'bg-slate-100 text-slate-700',
    };
  };

  const brand = getProviderBrand();

  return (
    <>
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5 flex flex-col justify-between hover:border-slate-300 transition-all space-y-4">
        {/* Top: Provider Logo & Status */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${brand.color} border border-slate-100`}>
              {brand.icon}
            </div>
            <div className="min-w-0">
              <h4 className="text-sm font-bold text-slate-900 truncate">
                {connection.name}
              </h4>
              <p className="text-xs text-slate-400 capitalize">
                {brand.label}
              </p>
            </div>
          </div>

          <span
            className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold shrink-0 ${
              isConnected
                ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                : 'bg-amber-50 text-amber-700 border border-amber-100'
            }`}
          >
            {isConnected ? 'Connected' : 'Setup Required'}
          </span>
        </div>

        {/* Middle: Storage Capacity & Usage */}
        <div className="space-y-2 py-1">
          <div className="flex justify-between items-baseline text-xs text-slate-500">
            <span>Storage Used</span>
            {usedBytes > 0 ? (
              <span className="font-semibold text-slate-800">
                {formatBytes(usedBytes)} of {formatBytes(capacityBytes)}
              </span>
            ) : (
              <span className="text-slate-400">Usage not reported</span>
            )}
          </div>
          {usedBytes > 0 && (
            <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-blue-600 rounded-full transition-all duration-300"
                style={{ width: `${usedPercent}%` }}
              />
            </div>
          )}

          {/* Last scanned date */}
          <div className="flex items-center gap-1.5 text-[11px] text-slate-400 pt-1">
            <Clock className="w-3.5 h-3.5 shrink-0" />
            <span>
              Last scanned: {connection.lastScannedAt ? formatDate(connection.lastScannedAt) : 'Never'}
            </span>
          </div>
        </div>

        {/* Bottom Actions: Scan Button & Disconnect */}
        <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
          {isConnected ? (
            <Button
              variant="primary"
              size="sm"
              icon={Sparkles}
              onClick={() => onTriggerScan && onTriggerScan(connection)}
              className="bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium"
            >
              Scan
            </Button>
          ) : (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setModalOpen(true)}
              className="text-xs border-slate-200 text-slate-700 hover:bg-slate-50"
            >
              Connect Account
            </Button>
          )}

          {isLocal ? (
            <button
              onClick={() => setModalOpen(true)}
              className="text-xs text-slate-500 hover:text-slate-800 font-medium transition-colors"
            >
              Manage Folders
            </button>
          ) : isConnected ? (
            <button
              onClick={handleDisconnect}
              className="text-xs text-slate-400 hover:text-rose-600 font-medium transition-colors"
            >
              Disconnect
            </button>
          ) : null}
        </div>
      </div>

      {modalOpen && (
        <ConnectModal
          isOpen={modalOpen}
          onClose={() => setModalOpen(false)}
          defaultProvider={type}
        />
      )}
    </>
  );
};

export default StorageCard;
