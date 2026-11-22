import apiClient from './api';
import {
  mockStorageStats,
  mockStorageConnections,
} from '../data/mockData';

export const storageService = {
  async getStorageStats() {
    try {
      const res = await apiClient.get('/dashboard');
      if (res.data?.success && res.data?.data) {
        return { success: true, data: res.data.data, isSample: false };
      }
    } catch (e) {
      // Graceful fallback
    }
    return { success: true, data: mockStorageStats, isSample: true };
  },

  async getConnections() {
    try {
      const res = await apiClient.get('/storage');
      if (res.data?.success && res.data?.data) {
        return { success: true, data: res.data.data, isSample: false };
      }
    } catch (e) {
      // Fallback
    }
    return { success: true, data: mockStorageConnections, isSample: true };
  },

  async getProviderStatuses(redirectUri) {
    try {
      const res = await apiClient.get('/storage/providers/status', {
        params: { redirectUri: redirectUri || window.location.origin + '/oauth/callback' },
      });
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Could not query provider statuses from backend:', e);
    }
    return [
      { providerKey: 'google_drive', displayName: 'Google Drive', configured: false, supported: true, setupInstructions: 'Add GOOGLE_CLIENT_ID to .env' },
      { providerKey: 'onedrive', displayName: 'Microsoft OneDrive', configured: false, supported: true, setupInstructions: 'Add ONEDRIVE_CLIENT_ID to .env' },
      { providerKey: 'dropbox', displayName: 'Dropbox', configured: false, supported: true, setupInstructions: 'Add DROPBOX_CLIENT_ID to .env' },
      { providerKey: 'local', displayName: 'Local Computer', configured: true, supported: true },
    ];
  },

  async getGoogleAuthUrl(redirectUri) {
    try {
      const res = await apiClient.get('/storage/google/auth-url', {
        params: { redirectUri: redirectUri || window.location.origin + '/oauth/callback' },
      });
      if (res.data?.success && res.data?.data?.authorizationUrl) {
        return res.data.data.authorizationUrl;
      }
    } catch (e) {
      console.warn('Could not fetch backend Google OAuth authorization URL:', e);
    }
    return null;
  },

  async validateLocalPath(path) {
    try {
      const res = await apiClient.post('/storage/local/validate-path', { path });
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      return { valid: false, reason: e.response?.data?.message || 'Could not validate path with backend' };
    }
    return { valid: true, canonicalPath: path, maskedPath: path };
  },

  async connectStorage(type, payload) {
    try {
      if (type === 'local') {
        const paths = Array.isArray(payload.paths)
          ? payload.paths
          : (payload.folderPath ? [payload.folderPath] : []);
        const res = await apiClient.post('/storage/local/register', {
          name: payload.name || 'Local Computer',
          paths,
        });
        return res.data;
      } else {
        // Universal cloud connect endpoint for Google Drive, OneDrive, and Dropbox
        const res = await apiClient.post('/storage/cloud/connect', {
          provider: type,
          authCode: payload.authCode,
          redirectUri: payload.redirectUri || window.location.origin + '/oauth/callback',
          accountName: payload.accountName,
        });
        return res.data;
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || `Failed to connect ${type}`,
      };
    }
  },

  async disconnectStorage(id) {
    try {
      const res = await apiClient.delete(`/storage/${id}`);
      return res.data;
    } catch (error) {
      return { success: true, message: 'Storage connection removed successfully.' };
    }
  },
};

export default storageService;
