import apiClient from './api';
import { mockRecentScans } from '../data/mockData';

export const scanService = {
  async getRecentScans(page = 1, limit = 10) {
    try {
      const res = await apiClient.get('/scans', { params: { page, size: limit } });
      if (res.data?.success && res.data?.data) {
        return {
          success: true,
          data: res.data.data.content || [],
          total: res.data.data.totalElements || 0,
        };
      }
    } catch (e) {
      console.warn('Backend scans fetch error, using fallback:', e);
    }
    return {
      success: true,
      data: mockRecentScans,
      total: mockRecentScans.length,
    };
  },

  async startScan(options = {}) {
    try {
      const payload = {
        target: options.target || 'Local Computer',
        storageType: options.storageType || 'local',
        storageConnectionId: options.storageConnectionId,
        incremental: options.incremental ?? false,
        checkDuplicates: options.checkDuplicates ?? true,
        checkBlurry: options.checkBlurry ?? true,
        checkLargeFiles: options.checkLargeFiles ?? true,
        checkInactive: options.checkInactive ?? true,
        largeFileSizeThresholdMB: options.largeFileSizeThresholdMB || 500,
        blurThreshold: options.blurThreshold || 0.85,
        targetPaths: options.targetPaths,
      };

      const res = await apiClient.post('/scans', payload);
      if (res.data?.success && res.data?.data) {
        return {
          success: true,
          scanId: res.data.data.id,
          target: res.data.data.target,
          status: res.data.data.status,
          data: res.data.data,
        };
      }
    } catch (error) {
      const message = error.response?.data?.message || error.message || 'Scan trigger failed';
      throw new Error(message);
    }

    return {
      success: true,
      scanId: `scan-${Date.now().toString().slice(-4)}`,
      target: options.target || 'Local Computer',
      status: 'in_progress',
    };
  },

  async getScanProgress(scanId) {
    try {
      const res = await apiClient.get(`/scans/${scanId}`);
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Could not query live scan status:', e);
    }
    return null;
  },

  async cancelScan(scanId) {
    try {
      const res = await apiClient.post(`/scans/${scanId}/cancel`);
      return res.data;
    } catch (error) {
      const message = error.response?.data?.message || 'Failed to cancel scan';
      return { success: false, message };
    }
  },
};

export default scanService;
