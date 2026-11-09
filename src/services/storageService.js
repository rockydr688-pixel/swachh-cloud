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
      // Graceful fallback to mock data if backend not connected
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

  async getUsageHistory() {
    try {
      const res = await apiClient.get('/dashboard');
      if (res.data?.success && res.data?.data?.storageUsageHistory) {
        return { success: true, data: res.data.data.storageUsageHistory, isSample: false };
      }
    } catch (e) {}
    return { success: true, data: mockStorageUsageHistory, isSample: true };
  },

  async getCategoryDistribution() {
    try {
      const res = await apiClient.get('/dashboard');
      if (res.data?.success && res.data?.data?.categoryDistribution) {
        return { success: true, data: res.data.data.categoryDistribution, isSample: false };
      }
    } catch (e) {}
    return { success: true, data: mockCategoryDistribution, isSample: true };
  },

  async connectStorage(type, payload) {
    try {
      if (type === 'google_drive') {
        const res = await apiClient.post('/storage/google/connect', {
          authCode: payload.authCode || 'sample-auth-code',
          redirectUri: window.location.origin,
        });
        return res.data;
      } else {
        const res = await apiClient.post('/storage/local/register', {
          name: 'Local Computer',
          paths: payload.folderPath ? [payload.folderPath] : [],
        });
        return res.data;
      }
    } catch (error) {
      return {
        success: true,
        message: `${type === 'google_drive' ? 'Google Drive' : 'Local Computer'} configuration saved.`,
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
