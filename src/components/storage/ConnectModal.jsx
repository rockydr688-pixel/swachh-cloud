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
  AlertCircle,
  ExternalLink,
  ShieldCheck,
  CheckCircle2,
  Info,
  Lock,
  RefreshCw,
} from 'lucide-react';

export const ConnectModal = ({
  isOpen,
  onClose,
  type = 'local', // 'local' | 'google_drive'
  onSuccess,
}) => {
  const { connectStorage } = useStorage();
  const { addToast } = useToast();

  const [folderPath, setFolderPath] = useState('C:/Users/rocky/Downloads');
  const [pathValidation, setPathValidation] = useState(null);
  const [validatingPath, setValidatingPath] = useState(false);

  // Google OAuth state
  const [authCode, setAuthCode] = useState('');
  const [authUrl, setAuthUrl] = useState('');
  const [loading, setLoading] = useState(false);

  const isLocal = type === 'local';

  useEffect(() => {
    if (isOpen && !isLocal) {
      storageService.getGoogleAuthUrl().then((url) => {
        setAuthUrl(url);
      });
    }
  }, [isOpen, isLocal]);

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

    setLoading(true);
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
      setLoading(false);
    }
  };

  const handleConnectGoogle = async (e) => {
    e.preventDefault();
    if (!authCode.trim()) {
      addToast({
        title: 'Authorization Code Required',
        message: 'Please authorize with Google and enter the code returned.',
        type: 'warning',
      });
      return;
    }

    setLoading(true);
    try {
      const res = await connectStorage('google_drive', {
        authCode: authCode.trim(),
      });

      if (res?.success) {
        addToast({
          title: 'Google Drive Connected',
          message: 'OAuth 2.0 authorization verified. Drive metadata is now synchronized.',
          type: 'success',
        });
        if (onSuccess) onSuccess();
        onClose();
      } else {
        addToast({
          title: 'OAuth Verification Failed',
          message: res?.message || 'Could not exchange authorization code with Google.',
          type: 'error',
        });
      }
    } catch (err) {
      addToast({
        title: 'Google Drive Error',
        message: err.message || 'Google Drive connection failed',
        type: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isLocal ? 'Configure Local Storage Folders' : 'Connect Google Drive (OAuth 2.0)'}
      subtitle={
        isLocal
          ? 'Link local filesystem directories monitored by the lightweight agent'
          : 'Enterprise Google Drive OAuth 2.0 Authorization'
      }
      maxWidth="max-w-lg"
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
              onClick={handleConnectLocal}
              className="bg-indigo-600 hover:bg-indigo-700"
            >
              Add Folder to Agent
            </Button>
          ) : (
            <Button
              variant="primary"
              size="sm"
              isLoading={loading}
              onClick={handleConnectGoogle}
              disabled={!authCode.trim()}
              className="bg-violet-600 hover:bg-violet-700"
            >
              Verify & Connect Drive
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
                Local storage optimization works directly with your local background agent to read attributes, compute cryptographic SHA-256 hashes, and scan media without uploading files to any external server.
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
                  onChange={(e) => {
                    setFolderPath(e.target.value);
                    setPathValidation(null);
                  }}
                  placeholder="e.g. C:/Users/name/Downloads or /home/name/Downloads"
                  className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 font-mono"
                />
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={handleValidatePath}
                  isLoading={validatingPath}
                  className="shrink-0 text-xs"
                >
                  Validate
                </Button>
              </div>

              {pathValidation && (
                <div
                  className={`mt-2 p-2 rounded-lg text-xs flex items-center gap-2 border ${
                    pathValidation.valid
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                      : 'bg-rose-50 text-rose-800 border-rose-200'
                  }`}
                >
                  {pathValidation.valid ? (
                    <>
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                      <span>Path validated and accessible: <strong className="font-mono">{pathValidation.maskedPath}</strong></span>
                    </>
                  ) : (
                    <>
                      <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                      <span>{pathValidation.reason || 'Invalid directory'}</span>
                    </>
                  )}
                </div>
              )}

              <p className="text-[11px] text-slate-400 mt-1.5">
                Subdirectories will be recursively scanned. System folders (Windows/System32, /etc) are strictly rejected.
              </p>
            </div>
          </>
        ) : (
          <>
            <div className="p-3 bg-violet-50/70 border border-violet-100 rounded-xl text-xs text-violet-900 space-y-2">
              <div className="flex items-center gap-2 font-bold text-violet-950">
                <ShieldCheck className="w-4 h-4 text-violet-600 shrink-0" />
                <span>Minimum Privilege OAuth 2.0 Architecture</span>
              </div>
              <p className="text-violet-800 text-[11px] leading-relaxed">
                CloudClean AI requests read-only metadata permissions (<code className="font-mono text-[10px]">drive.metadata.readonly</code>) to calculate duplicate and inactive storage without modifying files without explicit consent.
              </p>
            </div>

            {/* Step 1: Open Google Consent Screen */}
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-2">
              <span className="font-semibold text-slate-800 block">Step 1: Sign in with Google</span>
              <p className="text-slate-500 text-[11px]">
                Click below to open Google's secure authorization screen in a new tab:
              </p>
              <Button
                variant="subtleViolet"
                size="sm"
                icon={ExternalLink}
                onClick={() => window.open(authUrl, '_blank', 'noopener,noreferrer')}
                className="w-full justify-center"
              >
                Open Google Consent Screen
              </Button>
            </div>

            {/* Step 2: Paste Authorization Code */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Step 2: Enter Authorization Code
              </label>
              <input
                type="text"
                value={authCode}
                onChange={(e) => setAuthCode(e.target.value)}
                placeholder="4/0A..."
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-500 font-mono"
              />
              <p className="text-[11px] text-slate-400 mt-1">
                Paste the verification code generated after approving the Google Drive access prompt.
              </p>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
};

export default ConnectModal;
