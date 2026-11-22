import React, { useState, useEffect } from 'react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import Badge from '../common/Badge';
import { storageService } from '../../services/storageService';
import { useStorage } from '../../context/StorageContext';
import { useToast } from '../../context/ToastContext';
import {
  FolderPlus,
  Cloud,
  Layers,
  Box,
  HardDrive,
  AlertCircle,
  ExternalLink,
  ShieldCheck,
  CheckCircle2,
  Info,
  Lock,
  RefreshCw,
  Settings,
} from 'lucide-react';

export const ConnectModal = ({
  isOpen,
  onClose,
  type = 'local',
  onSuccess,
}) => {
  const { connectStorage } = useStorage();
  const { addToast } = useToast();

  const [activeType, setActiveType] = useState(type || 'local');
  const [folderPath, setFolderPath] = useState('C:/Users/rocky/Downloads');
  const [pathValidation, setPathValidation] = useState(null);
  const [validatingPath, setValidatingPath] = useState(false);

  // Cloud OAuth state
  const [authCode, setAuthCode] = useState('');
  const [accountName, setAccountName] = useState('');
  const [providerStatuses, setProviderStatuses] = useState([]);
  const [loadingStatuses, setLoadingStatuses] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (type) {
      setActiveType(type);
    }
  }, [type]);

  useEffect(() => {
    if (isOpen) {
      setLoadingStatuses(true);
      storageService.getProviderStatuses().then((statuses) => {
        setProviderStatuses(statuses || []);
        setLoadingStatuses(false);
      }).catch(() => setLoadingStatuses(false));
    }
  }, [isOpen]);

  const currentProviderStatus = providerStatuses.find(p => p.providerKey === activeType) || {
    providerKey: activeType,
    displayName: activeType,
    configured: activeType === 'local',
    supported: true,
  };

  const isLocal = activeType === 'local';

  const handleValidatePath = async () => {
    if (!folderPath) return;
    setValidatingPath(true);
    const result = await storageService.validateLocalPath(folderPath);
    setPathValidation(result);
    setValidatingPath(false);
  };

  const handleConnectLocal = async (e) => {
    e.preventDefault();
    if (!folderPath) return;

    setSubmitting(true);
    try {
      const res = await connectStorage('local', {
        name: 'Local Computer',
        folderPath,
      });

      if (res?.success) {
        addToast({
          title: 'Local Storage Connected',
          message: `Folder '${folderPath}' added to local monitoring agent.`,
          type: 'success',
        });
        if (onSuccess) onSuccess();
        onClose();
      } else {
        addToast({
          title: 'Connection Error',
          message: res?.message || 'Could not register local folder.',
          type: 'error',
        });
      }
    } catch (err) {
      addToast({
        title: 'Connection Error',
        message: err.message || 'Failed to connect local storage',
        type: 'error',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleConnectCloud = async (e) => {
    e.preventDefault();
    if (!authCode.trim()) {
      addToast({
        title: 'Authorization Code Required',
        message: 'Please authorize with the provider and paste the returned authorization code.',
        type: 'warning',
      });
      return;
    }

    setSubmitting(true);
    try {
      const res = await connectStorage(activeType, {
        authCode: authCode.trim(),
        redirectUri: window.location.origin + '/oauth/callback',
        accountName: accountName.trim() || undefined,
      });

      if (res?.success) {
        addToast({
          title: 'Cloud Storage Connected',
          message: `${currentProviderStatus.displayName} account connected successfully.`,
          type: 'success',
        });
        setAuthCode('');
        setAccountName('');
        if (onSuccess) onSuccess();
        onClose();
      } else {
        addToast({
          title: 'OAuth Error',
          message: res?.message || `Could not connect to ${currentProviderStatus.displayName}.`,
          type: 'error',
        });
      }
    } catch (err) {
      addToast({
        title: 'Connection Error',
        message: err.message || `Failed to connect ${currentProviderStatus.displayName}`,
        type: 'error',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const openAuthConsent = () => {
    if (currentProviderStatus.authorizationUrl) {
      window.open(currentProviderStatus.authorizationUrl, '_blank', 'width=650,height=750');
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Connect Storage Provider"
      subtitle="Link physical filesystem folders or actual enterprise cloud repositories"
      size="lg"
    >
      <div className="space-y-5">
        {/* Provider Tabs */}
        <div className="grid grid-cols-4 gap-2 p-1 bg-slate-100 rounded-xl">
          {[
            { key: 'local', label: 'Local PC', icon: HardDrive },
            { key: 'google_drive', label: 'Google Drive', icon: Cloud },
            { key: 'onedrive', label: 'OneDrive', icon: Layers },
            { key: 'dropbox', label: 'Dropbox', icon: Box },
          ].map((tab) => {
            const Icon = tab.icon;
            const isSelected = activeType === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => {
                  setActiveType(tab.key);
                  setPathValidation(null);
                  setAuthCode('');
                }}
                className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-semibold transition-all ${
                  isSelected
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Local Computer Form */}
        {isLocal && (
          <form onSubmit={handleConnectLocal} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700 block">
                Local Folder Path
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={folderPath}
                  onChange={(e) => {
                    setFolderPath(e.target.value);
                    setPathValidation(null);
                  }}
                  placeholder="e.g. C:/Users/rocky/Downloads or /home/user/downloads"
                  className="flex-1 px-3 py-2 text-xs font-mono rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={handleValidatePath}
                  disabled={validatingPath || !folderPath}
                >
                  {validatingPath ? 'Validating...' : 'Validate Path'}
                </Button>
              </div>
            </div>

            {pathValidation && (
              <div
                className={`p-3 rounded-lg border text-xs flex items-start gap-2.5 ${
                  pathValidation.valid
                    ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                    : 'bg-rose-50 text-rose-800 border-rose-200'
                }`}
              >
                {pathValidation.valid ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                )}
                <div>
                  <span className="font-semibold block">
                    {pathValidation.valid ? 'Valid Local Directory' : 'Invalid Path'}
                  </span>
                  <p className="mt-0.5 font-mono text-[11px]">
                    {pathValidation.valid
                      ? pathValidation.canonicalPath
                      : pathValidation.reason}
                  </p>
                </div>
              </div>
            )}

            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-600 space-y-1">
              <span className="font-semibold text-slate-800 block">Local Agent Privacy Guard</span>
              <p>
                Files are read recursively via local IPC daemon. Zero file bytes are transmitted outside your local machine.
              </p>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="ghost" onClick={onClose}>
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                disabled={submitting || (pathValidation && !pathValidation.valid)}
              >
                {submitting ? 'Registering...' : 'Add Monitored Folder'}
              </Button>
            </div>
          </form>
        )}

        {/* Cloud Provider Form (Google Drive, OneDrive, Dropbox) */}
        {!isLocal && (
          <div className="space-y-4">
            {/* If Provider is NOT Configured: Display Guided Configuration Instructions */}
            {!currentProviderStatus.configured ? (
              <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 space-y-3">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-5 h-5 text-amber-600 shrink-0" />
                  <h4 className="font-bold text-sm">
                    {currentProviderStatus.displayName} Integration Awaiting Configuration
                  </h4>
                </div>
                <p className="text-xs text-amber-800 leading-relaxed">
                  Actual cloud scanning requires official API credentials. No mock integration will be used.
                </p>
                <div className="p-3 bg-white/80 rounded-lg border border-amber-200 font-mono text-xs space-y-1 text-slate-800">
                  <p className="font-bold text-amber-900">Setup Instructions:</p>
                  <p className="text-slate-600">{currentProviderStatus.setupInstructions}</p>
                  <div className="mt-2 text-[11px] text-slate-500">
                    {activeType === 'google_drive' && (
                      <p>Add <code>GOOGLE_CLIENT_ID</code> and <code>GOOGLE_CLIENT_SECRET</code> to your <code>.env</code> file.</p>
                    )}
                    {activeType === 'onedrive' && (
                      <p>Register an Azure App in Microsoft Entra and add <code>ONEDRIVE_CLIENT_ID</code> and <code>ONEDRIVE_CLIENT_SECRET</code> to <code>.env</code>.</p>
                    )}
                    {activeType === 'dropbox' && (
                      <p>Create a scoped Dropbox App in App Console and add <code>DROPBOX_CLIENT_ID</code> and <code>DROPBOX_CLIENT_SECRET</code> to <code>.env</code>.</p>
                    )}
                  </div>
                </div>
                <div className="flex justify-end pt-1">
                  <Button variant="secondary" size="sm" onClick={onClose}>
                    Close
                  </Button>
                </div>
              </div>
            ) : (
              /* If Provider IS Configured: Real OAuth 2.0 Flow */
              <form onSubmit={handleConnectCloud} className="space-y-4">
                <div className="p-3.5 bg-indigo-50/70 border border-indigo-100 rounded-xl text-xs space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-indigo-950 flex items-center gap-1.5">
                      <ShieldCheck className="w-4 h-4 text-indigo-600" />
                      Step 1: Authorize with {currentProviderStatus.displayName}
                    </span>
                    <Badge variant="success" size="sm">
                      Ready to Connect
                    </Badge>
                  </div>
                  <p className="text-indigo-800 leading-relaxed text-[11px]">
                    Click the button below to sign in with your actual {currentProviderStatus.displayName} account. Requesting minimum metadata read/trash scopes.
                  </p>
                  <Button
                    type="button"
                    variant="primary"
                    size="sm"
                    icon={ExternalLink}
                    onClick={openAuthConsent}
                    className="w-full justify-center bg-indigo-600 hover:bg-indigo-700"
                  >
                    Open {currentProviderStatus.displayName} OAuth Login
                  </Button>
                </div>

                <div className="space-y-3 pt-1">
                  <span className="font-bold text-slate-800 text-xs block">
                    Step 2: Enter Authorization Code & Account Label
                  </span>

                  <div>
                    <label className="text-xs font-semibold text-slate-700 block mb-1">
                      Account Label (Optional)
                    </label>
                    <input
                      type="text"
                      value={accountName}
                      onChange={(e) => setAccountName(e.target.value)}
                      placeholder="e.g. Personal Account, Work Files, Research Drive"
                      className="w-full px-3 py-2 text-xs rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="text-xs font-semibold text-slate-700 block mb-1">
                      Authorization Code
                    </label>
                    <input
                      type="text"
                      value={authCode}
                      onChange={(e) => setAuthCode(e.target.value)}
                      placeholder="Paste the authorization code from OAuth callback"
                      className="w-full px-3 py-2 text-xs font-mono rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                    />
                  </div>
                </div>

                <div className="flex justify-end gap-3 pt-2">
                  <Button type="button" variant="ghost" onClick={onClose}>
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="primary"
                    disabled={submitting || !authCode.trim()}
                  >
                    {submitting ? 'Connecting...' : `Connect ${currentProviderStatus.displayName}`}
                  </Button>
                </div>
              </form>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
};

export default ConnectModal;
