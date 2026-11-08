import { mockRecentScans } from '../data/mockData';

const MOCK_DELAY = parseInt(import.meta.env.VITE_MOCK_API_DELAY || '350', 10);
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const scanService = {
  async getRecentScans() {
    await delay(MOCK_DELAY);
    return {
      success: true,
      data: mockRecentScans,
    };
  },

  async startScan(options = {}) {
    await delay(MOCK_DELAY + 200);
    return {
      success: true,
      scanId: `scan-${Date.now().toString().slice(-4)}`,
      target: options.target || 'Local Computer',
      status: 'in_progress',
    };
  },
};

export default scanService;

