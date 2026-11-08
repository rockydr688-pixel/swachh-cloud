import React, { useState } from 'react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import Badge from '../common/Badge';
import {
  FolderPlus,
  Cloud,
  AlertCircle,
  ExternalLink,
  ShieldCheck,
  CheckCircle2,
  Info,
} from 'lucide-react';

export const ConnectModal = ({
  isOpen,
  onClose,
  type = 'local', // 'local' | 'google_drive'
  onConfirm,
}) => {
  const [folderPath, setFolderPath] = useState('C:/Users/rocky/Downloads');
  const [loading, setLoading] = useState(false);

  const isLocal = type === 'local';

  const handleConnect = async (e) => {
    e.preventDefault();
    setLoading(true);
    // Simulate brief action
    setTimeout(() => {
      setLoading(false);
      if (onConfirm) onConfirm(type, { folderPath });
      onClose();
    }, 600);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isLocal ? 'Connect Local Storage Agent' : 'Google Drive Cloud Integration'}
      subtitle={
        isLocal
          ? 'Link local directories via future CloudClean AI Daemon'
          : 'Enterprise OAuth 2.0 Google Workspace Connector'
      }
      maxWidth="max-w-md"
      footer={
        <div className="flex items-center justify-end gap-2 w-full">
          <Button variant="ghost" size="sm" onClick={onClose}>
            Cancel
          </Button>
          {isLocal ? (
            <Button
              variant="primary"
              size="sm"
              isLoading={loading}
              onClick={handleConnect}
            >
              Add Folder to Agent
            </Button>
          ) : (
            <Button
              variant="secondary"
              size="sm"
              disabled
              title="Real backend OAuth required"
            >
              OAuth Server Required
            </Button>
          )}
        </div>
      }
    >
      <div className="space-y-4">
        {isLocal ? (
          <>
            <div className="p-3 bg-indigo-50/70 border border-indigo-100 rounded-xl text-xs text-indigo-900 flex items-start gap-2.5">
              <Info className="w-4 h-4 text-indigo-600 shrink-0 mt-0.5" />
              <p>
                Local storage optimization works in tandem with the CloudClean AI background agent (daemon) to read hashes, run blur diagnostics, and index directories without uploading private files to the cloud.
              </p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Directory Path to Monitor
              </label>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={folderPath}
                  onChange={(e) => setFolderPath(e.target.value)}
                  placeholder="e.g. C:/Users/name/Downloads"
                  className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>
              <p className="text-[11px] text-slate-400 mt-1">
                Subdirectories will be recursively mapped for duplicate and blurry media scans.
              </p>
            </div>
          </>
        ) : (
          <>
            {/* Google Drive Card notice - strictly not simulating fake success */}
            <div className="p-4 bg-amber-50 border border-amber-200/80 rounded-xl text-xs text-amber-900 space-y-2">
              <div className="flex items-center gap-2 font-bold text-amber-950">
                <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
                <span>Backend OAuth 2.0 Integration Required</span>
              </div>
              <p className="text-amber-800 leading-relaxed text-[11px]">
                To securely authenticate with Google Drive, real authorization tokens must be exchanged through a dedicated backend OAuth service with Google Cloud API credentials.
              </p>
              <div className="pt-1 text-[11px] text-amber-900 font-semibold">
                This frontend interface does not simulate a fake connected cloud state. Once the backend API is deployed in Part 2, users will be redirected to Google’s OAuth consent screen.
              </div>
            </div>

            <div className="p-3 bg-slate-50 border border-slate-100 rounded-xl text-xs space-y-2">
              <span className="font-semibold text-slate-700 block">Required OAuth Scopes:</span>
              <ul className="list-disc list-inside space-y-1 text-slate-500 font-mono text-[11px]">
                <li>https://www.googleapis.com/auth/drive.metadata.readonly</li>
                <li>https://www.googleapis.com/auth/drive.readonly</li>
              </ul>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
};

export default ConnectModal;

