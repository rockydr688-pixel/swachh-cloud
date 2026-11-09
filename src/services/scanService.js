import apiClient from './api';
import { mockRecentScans } from '../data/mockData';

export const scanService = {
  async getRecentScans(page = 1, limit = 10) {
    try {
      const res = await apiClient.get('/scans', { params: { page, limit } });
      if (res.data?.success && res.data?.data) {
        return {
          success: true,
          data: res.data.data.content || [],
        };
      }
    } catch (e) {}
    return {
      success: true,
      data: mockRecentScans,
    };
  },

  async startScan(options = {}) {
    try {
      const res = await apiClient.post('/scans', {
        target: options.target || 'Local Computer',
        storageType: options.storageType || 'local',
        checkDuplicates: options.checkDuplicates ?? true,
        checkBlurry: options.checkBlurry ?? true,
        checkLargeFiles: options.checkLargeFiles ?? true,
        checkInactive: options.checkInactive ?? true,
      });

      if (res.data?.success && res.data?.data) {
        return {
          success: true,
          scanId: res.data.data.id,
          target: res.data.data.target,
          status: res.data.data.status,
        };
      }
    } catch (e) {}

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
    } catch (e) {}
    return null;
  },

  async cancelScan(scanId) {
    try {
      const res = await apiClient.post(`/scans/${scanId}/cancel`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'Scan cancelled' };
    }
  },
};

export default scanService;
