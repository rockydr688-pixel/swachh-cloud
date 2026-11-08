import apiClient from './api';
import {
  mockFiles,
  mockCleanupRecommendations,
  mockCleanupHistory,
} from '../data/mockData';

const MOCK_DELAY = parseInt(import.meta.env.VITE_MOCK_API_DELAY || '350', 10);
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fileService = {
  async getFiles(params = {}) {
    const {
      category = 'all',
      search = '',
      sortBy = 'size',
      sortOrder = 'desc',
      page = 1,
      limit = 10,
    } = params;

    await delay(MOCK_DELAY);

    let result = [...mockFiles];

    // Filter by category
    if (category && category !== 'all') {
      result = result.filter((file) => file.category === category);
    }

    // Filter by search query
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(
        (f) =>
          f.name.toLowerCase().includes(q) ||
          f.storageLocation.toLowerCase().includes(q) ||
          f.detectionReason.toLowerCase().includes(q) ||
          f.recommendationStatus.toLowerCase().includes(q)
      );
    }

    // Sorting
    result.sort((a, b) => {
      let valA, valB;
      if (sortBy === 'size') {
        valA = a.sizeBytes;
        valB = b.sizeBytes;
      } else if (sortBy === 'name') {
        valA = a.name.toLowerCase();
        valB = b.name.toLowerCase();
      } else if (sortBy === 'date') {
        valA = new Date(a.lastModified).getTime();
        valB = new Date(b.lastModified).getTime();
      } else if (sortBy === 'score' && a.blurScore !== undefined) {
        valA = a.blurScore ?? 0;
        valB = b.blurScore ?? 0;
      } else {
        valA = a.name;
        valB = b.name;
      }

      if (valA < valB) return sortOrder === 'asc' ? -1 : 1;
      if (valA > valB) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    // Pagination
    const totalCount = result.length;
    const startIndex = (page - 1) * limit;
    const paginatedItems = result.slice(startIndex, startIndex + limit);

    return {
      success: true,
      data: paginatedItems,
      pagination: {
        page,
        limit,
        totalCount,
        totalPages: Math.ceil(totalCount / limit) || 1,
      },
    };
  },

  async getRecommendations() {
    await delay(MOCK_DELAY);
    return {
      success: true,
      data: mockCleanupRecommendations,
    };
  },

  async getCleanupHistory() {
    await delay(MOCK_DELAY);
    return {
      success: true,
      data: mockCleanupHistory,
    };
  },

  async executeCleanup(recommendationId) {
    await delay(MOCK_DELAY + 400);
    return {
      success: true,
      message: 'Cleanup operation executed successfully. Storage updated.',
    };
  },

  async deleteFile(fileId) {
    await delay(MOCK_DELAY);
    return {
      success: true,
      message: 'File removed or moved to trash.',
    };
  },
};

export default fileService;

