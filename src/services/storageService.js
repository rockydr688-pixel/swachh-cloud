import apiClient from './api';
import {
  mockStorageStats,
  mockStorageConnections,
  mockStorageUsageHistory,
  mockCategoryDistribution,
} from '../data/mockData';

const MOCK_DELAY = parseInt(import.meta.env.VITE_MOCK_API_DELAY || '350', 10);
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const storageService = {
  async getStorageStats() {
    try {
      // In production/backend mode: const res = await apiClient.get('/storage/stats'); return res.data;
      await delay(MOCK_DELAY);
      return { success: true, data: mockStorageStats, isSample: true };
    } catch (error) {
      throw error;
    }
  },

  async getConnections() {
    try {
      await delay(MOCK_DELAY);
      return { success: true, data: mockStorageConnections, isSample: true };
    } catch (error) {
      throw error;
    }
  },

  async getUsageHistory() {
    try {
      await delay(MOCK_DELAY);
      return { success: true, data: mockStorageUsageHistory, isSample: true };
    } catch (error) {
      throw error;
    }
  },

  async getCategoryDistribution() {
    try {
      await delay(MOCK_DELAY);
      return { success: true, data: mockCategoryDistribution, isSample: true };
    } catch (error) {
      throw error;
    }
  },

  async connectStorage(type, payload) {
    try {
      await delay(MOCK_DELAY + 200);
      return {
        success: true,
        message: `${type === 'google_drive' ? 'Google Drive' : 'Local Computer'} configuration saved.`,
      };
    } catch (error) {
      throw error;
    }
  },

  async disconnectStorage(id) {
    try {
      await delay(MOCK_DELAY);
      return { success: true, message: 'Storage connection removed successfully.' };
    } catch (error) {
      throw error;
    }
  },
};

export default storageService;

