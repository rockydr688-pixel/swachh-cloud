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
  Lock,
  ChevronDown,
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

  // Cloud account state
  const [accountEmail, setAccountEmail] = useState('user@gmail.com');
  const [accountName, setAccountName] = useState('');
  const [authCode, setAuthCode] = useState('');
  const [providerStatuses, setProviderStatuses] = useState([]);
  const [loadingStatuses, setLoadingStatuses] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const getDefaultEmail = (pType) => {
    switch (pType) {
      case 'google_drive':
        return 'user@gmail.com';
      case 'onedrive':
        return 'user@outlook.com';
      case 'dropbox':
        return 'user@dropbox.com';
      default:
        return 'user@example.com';
    }
  };

  useEffect(() => {
    if (type) {
      setActiveType(type);
      setAccountEmail(getDefaultEmail(type));
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
    displayName: activeType === 'google_drive' ? 'Google Drive' : (activeType === 'onedrive' ? 'Microsoft OneDrive' : (activeType === 'dropbox' ? 'Dropbox' : 'Local Computer')),
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

  const handleOneClickConnect = async (providerKey) => {
    if (currentProviderStatus.configured && currentProviderStatus.authorizationUrl) {
      window.open(currentProviderStatus.authorizationUrl, '_blank', 'width=650,height=750');
      return;
    }

    const emailToUse = accountEmail.trim() || getDefaultEmail(providerKey);
    const labelToUse = accountName.trim() || `${currentProviderStatus.displayName} (${emailToUse})`;

    setSubmitting(true);
    try {
      const res = await connectStorage(providerKey, {
        authCode: emailToUse,
        accountName: labelToUse,
      });

      if (res?.success) {
        addToast({
          title: 'Storage Connected',
          message: `${currentProviderStatus.displayName} account connected successfully.`,
          type: 'success',
        });
        if (onSuccess) onSuccess();
        onClose();
      } else {
        addToast({
          title: 'Connection Error',
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

  const handleConnectCloud = async (e) => {
    e.preventDefault();
    const emailToUse = accountEmail.trim() || getDefaultEmail(activeType);
    const labelToUse = accountName.trim() || `${currentProviderStatus.displayName} (${emailToUse})`;
    const codeToUse = authCode.trim() || emailToUse;

    setSubmitting(true);
    try {
      const res = await connectStorage(activeType, {
        authCode: codeToUse,
        redirectUri: window.location.origin + '/oauth/callback',
        accountName: labelToUse,
      });

      if (res?.success) {
        addToast({
          title: 'Storage Connected',
          message: `${currentProviderStatus.displayName} account connected successfully.`,
          type: 'success',
        });
        setAuthCode('');
        setAccountName('');
        if (onSuccess) onSuccess();
        onClose();
      } else {
        addToast({
          title: 'Connection Error',
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
                  setAccountEmail(getDefaultEmail(tab.key));
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
                  className="flex-1 px-3 py-2 text-xs font-mono rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
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

        {/* Cloud Provider Direct Connect (Google Drive, OneDrive, Dropbox) */}
        {!isLocal && (
          <div className="space-y-4">
            {/* Header info */}
            <div className="flex items-center gap-3 p-3 bg-blue-50/60 border border-blue-100 rounded-xl">
              <div className="w-10 h-10 rounded-xl bg-white shadow-sm flex items-center justify-center shrink-0 border border-blue-100">
                {activeType === 'google_drive' && (
                  <svg className="w-6 h-6" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
                  </svg>
                )}
                {activeType === 'onedrive' && (
                  <svg className="w-5 h-5" viewBox="0 0 23 23">
                    <path fill="#f35325" d="M1 1h10v10H1z"/>
                    <path fill="#81bc06" d="M12 1h10v10H12z"/>
                    <path fill="#05a6f0" d="M1 12h10v10H1z"/>
                    <path fill="#ffba08" d="M12 12h10v10H12z"/>
                  </svg>
                )}
                {activeType === 'dropbox' && (
                  <svg className="w-5 h-5 text-[#0061FF]" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 2l6 3.9 6-3.9 6 3.9-6 3.9 6 3.9-6 3.9-6-3.9-6 3.9-6-3.9 6-3.9L0 5.9 6 2zm6 13.7l6-3.9 6 3.9-6 3.9-6-3.9zm0 1.9l6 3.9-6 3.9-6-3.9 6-3.9z"/>
                  </svg>
                )}
              </div>
              <div>
                <h4 className="text-sm font-bold text-slate-900">
                  Connect {currentProviderStatus.displayName}
                </h4>
                <p className="text-xs text-slate-600">
                  Directly link your account to scan for duplicate files, blurry photos, and recoverable space.
                </p>
              </div>
            </div>

            {/* Direct One-Click Sign In Button */}
            <button
              type="button"
              onClick={() => handleOneClickConnect(activeType)}
              disabled={submitting}
              className="w-full flex items-center justify-center gap-3 py-3 px-4 bg-white border border-slate-300 hover:bg-slate-50 text-slate-800 font-semibold text-sm rounded-xl shadow-sm transition-all hover:shadow active:scale-[0.99]"
            >
              {activeType === 'google_drive' && (
                <>
                  <svg className="w-5 h-5 shrink-0" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
                  </svg>
                  <span>{submitting ? 'Connecting Google Account...' : 'Sign in with Google'}</span>
                </>
              )}
              {activeType === 'onedrive' && (
                <>
                  <svg className="w-5 h-5 shrink-0" viewBox="0 0 23 23">
                    <path fill="#f35325" d="M1 1h10v10H1z"/>
                    <path fill="#81bc06" d="M12 1h10v10H12z"/>
                    <path fill="#05a6f0" d="M1 12h10v10H1z"/>
                    <path fill="#ffba08" d="M12 12h10v10H12z"/>
                  </svg>
                  <span>{submitting ? 'Connecting Microsoft Account...' : 'Sign in with Microsoft'}</span>
                </>
              )}
              {activeType === 'dropbox' && (
                <>
                  <svg className="w-5 h-5 shrink-0 text-[#0061FF]" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 2l6 3.9 6-3.9 6 3.9-6 3.9 6 3.9-6 3.9-6-3.9-6 3.9-6-3.9 6-3.9L0 5.9 6 2zm6 13.7l6-3.9 6 3.9-6 3.9-6-3.9zm0 1.9l6 3.9-6 3.9-6-3.9 6-3.9z"/>
                  </svg>
                  <span>{submitting ? 'Connecting Dropbox Account...' : 'Sign in with Dropbox'}</span>
                </>
              )}
            </button>

            {/* Clean Divider */}
            <div className="relative flex py-1 items-center">
              <div className="flex-grow border-t border-slate-200"></div>
              <span className="flex-shrink mx-3 text-[11px] text-slate-400 font-semibold uppercase tracking-wider">
                or connect with account email
              </span>
              <div className="flex-grow border-t border-slate-200"></div>
            </div>

            {/* Direct Input Form */}
            <form onSubmit={handleConnectCloud} className="space-y-3.5">
              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1">
                  {currentProviderStatus.displayName} Account Email
                </label>
                <input
                  type="email"
                  value={accountEmail}
                  onChange={(e) => setAccountEmail(e.target.value)}
                  placeholder={`e.g. ${getDefaultEmail(activeType)}`}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                  required
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1">
                  Account Label (Optional)
                </label>
                <input
                  type="text"
                  value={accountName}
                  onChange={(e) => setAccountName(e.target.value)}
                  placeholder={`e.g. Personal ${currentProviderStatus.displayName}`}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>

              {/* Privacy Safety Note */}
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-600 flex items-start gap-2.5">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <p className="text-[11px] leading-relaxed">
                  <strong className="text-slate-800">Safe & Private:</strong> Requesting minimal read-only metadata and Trash permissions. Files are analyzed securely and never permanently deleted without your confirmation.
                </p>
              </div>

              {/* Action Buttons */}
              <div className="flex justify-end gap-3 pt-2">
                <Button type="button" variant="ghost" onClick={onClose}>
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  disabled={submitting}
                >
                  {submitting ? 'Connecting...' : `Connect ${currentProviderStatus.displayName}`}
                </Button>
              </div>
            </form>

            {/* Optional Collapsible Developer Section */}
            <details className="text-xs text-slate-500 rounded-lg border border-slate-200 bg-slate-50 p-2.5">
              <summary className="cursor-pointer font-medium text-slate-700 hover:text-slate-900 flex items-center justify-between">
                <span>Advanced: Custom OAuth API Credentials (Optional)</span>
                <span className="text-[10px] text-slate-400 font-normal">For Developers</span>
              </summary>
              <div className="mt-2.5 pt-2.5 border-t border-slate-200 space-y-2 font-mono text-[11px] text-slate-600">
                <p>To use custom developer OAuth credentials from your cloud console instead of direct web sign-in:</p>
                {activeType === 'google_drive' && (
                  <p className="text-slate-700">Set <code>GOOGLE_CLIENT_ID</code> and <code>GOOGLE_CLIENT_SECRET</code> in <code>.env</code></p>
                )}
                {activeType === 'onedrive' && (
                  <p className="text-slate-700">Set <code>ONEDRIVE_CLIENT_ID</code> and <code>ONEDRIVE_CLIENT_SECRET</code> in <code>.env</code></p>
                )}
                {activeType === 'dropbox' && (
                  <p className="text-slate-700">Set <code>DROPBOX_CLIENT_ID</code> and <code>DROPBOX_CLIENT_SECRET</code> in <code>.env</code></p>
                )}
                {currentProviderStatus.configured && currentProviderStatus.authorizationUrl && (
                  <button
                    type="button"
                    onClick={openAuthConsent}
                    className="mt-1 text-xs text-blue-600 hover:underline flex items-center gap-1 font-sans font-semibold"
                  >
                    <ExternalLink className="w-3.5 h-3.5" /> Open {currentProviderStatus.displayName} OAuth Consent Screen
                  </button>
                )}
              </div>
            </details>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default ConnectModal;
