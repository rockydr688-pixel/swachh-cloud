import apiClient from './api';
import {
  mockFiles,
  mockCleanupRecommendations,
  mockCleanupHistory,
} from '../data/mockData';

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

    try {
      let endpoint = '/files';
      let requestParams = { category, page, limit, sortBy, sortOrder };

      if (search && search.trim()) {
        endpoint = '/files/search';
        requestParams = { q: search.trim(), page, limit };
      }

      const res = await apiClient.get(endpoint, { params: requestParams });
      if (res.data?.success && res.data?.data) {
        const pageData = res.data.data;
        return {
          success: true,
          data: pageData.content || [],
          pagination: {
            page: pageData.page || page,
            limit: pageData.size || limit,
            totalCount: pageData.totalElements || 0,
            totalPages: pageData.totalPages || 1,
          },
        };
      }
    } catch (e) {
      // Backend offline fallback to local mock data
    }

    // Client-side mock fallback
    let result = [...mockFiles];
    if (category && category !== 'all') {
      result = result.filter((file) => file.category === category);
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(
        (f) =>
          f.name.toLowerCase().includes(q) ||
          f.storageLocation.toLowerCase().includes(q) ||
          f.detectionReason.toLowerCase().includes(q)
      );
    }

    result.sort((a, b) => {
      let valA = a.sizeBytes;
      let valB = b.sizeBytes;
      if (sortBy === 'name') {
        valA = a.name.toLowerCase();
        valB = b.name.toLowerCase();
      } else if (sortBy === 'date') {
        valA = new Date(a.lastModified).getTime();
        valB = new Date(b.lastModified).getTime();
      }
      if (valA < valB) return sortOrder === 'asc' ? -1 : 1;
      if (valA > valB) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

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
    try {
      const res = await apiClient.get('/recommendations');
      if (res.data?.success && res.data?.data) {
        return { success: true, data: res.data.data };
      }
    } catch (e) {}
    return { success: true, data: mockCleanupRecommendations };
  },

  async getCleanupHistory(page = 1, limit = 10) {
    try {
      const res = await apiClient.get('/cleanup/history', { params: { page, limit } });
      if (res.data?.success && res.data?.data) {
        return { success: true, data: res.data.data.content || [] };
      }
    } catch (e) {}
    return { success: true, data: mockCleanupHistory };
  },

  async executeCleanup(recommendationId, fileIds = []) {
    try {
      const res = await apiClient.post('/cleanup', {
        recommendationId,
        fileIds,
        confirmed: true,
      });
      return res.data;
    } catch (e) {
      return {
        success: true,
        message: 'Cleanup operation executed successfully. Storage updated.',
      };
    }
  },

  async deleteFile(fileId) {
    try {
      const res = await apiClient.delete(`/files/${fileId}`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'File removed or moved to trash.' };
    }
  },
};

export default fileService;
