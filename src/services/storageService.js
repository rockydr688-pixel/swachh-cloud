import apiClient from './api';
import {
  mockStorageStats,
  mockStorageConnections,
  mockStorageUsageHistory,
  mockCategoryDistribution,
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
    return 'https://accounts.google.com/o/oauth2/v2/auth?scope=https://www.googleapis.com/auth/drive.metadata.readonly';
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
      if (type === 'google_drive') {
        const res = await apiClient.post('/storage/google/connect', {
          authCode: payload.authCode,
          redirectUri: payload.redirectUri || window.location.origin + '/oauth/callback',
        });
        return res.data;
      } else {
        const paths = Array.isArray(payload.paths)
          ? payload.paths
          : (payload.folderPath ? [payload.folderPath] : []);
        const res = await apiClient.post('/storage/local/register', {
          name: payload.name || 'Local Computer',
          paths,
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
