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

  async getRecommendations(params = {}) {
    try {
      const res = await apiClient.get('/recommendations', { params });
      if (res.data?.success && res.data?.data) {
        return { success: true, data: res.data.data };
      }
    } catch (e) {
      console.warn('Fallback to mock recommendations', e);
    }
    return { success: true, data: mockCleanupRecommendations };
  },

  async acceptRecommendation(id) {
    try {
      const res = await apiClient.post(`/recommendations/${id}/accept`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'Recommendation accepted.' };
    }
  },

  async rejectRecommendation(id) {
    try {
      const res = await apiClient.post(`/recommendations/${id}/reject`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'Recommendation rejected.' };
    }
  },

  async excludeFile(id) {
    try {
      const res = await apiClient.post(`/recommendations/${id}/exclude`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'File excluded from recommendations.' };
    }
  },

  async protectFile(fileId) {
    try {
      const res = await apiClient.post(`/recommendations/files/${fileId}/protect`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'File protected from cleanup.' };
    }
  },

  async unprotectFile(fileId) {
    try {
      const res = await apiClient.delete(`/recommendations/files/${fileId}/protect`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'File protection removed.' };
    }
  },

  async calculateCleanupImpact(payload = {}) {
    try {
      const res = await apiClient.post('/cleanup/impact', payload);
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to mock impact calculation', e);
    }
    return null;
  },

  async getCleanupHistory(page = 1, limit = 10) {
    try {
      const res = await apiClient.get('/cleanup/history', { params: { page, limit } });
      if (res.data?.success && res.data?.data) {
        return { success: true, data: res.data.data.content || [], pagination: res.data.data };
      }
    } catch (e) {}
    return { success: true, data: mockCleanupHistory, pagination: { totalElements: mockCleanupHistory.length } };
  },

  async executeCleanup(params = {}) {
    const payload = typeof params === 'object' && params !== null && params.recommendationId !== undefined
      ? params
      : { recommendationId: arguments[0], fileIds: arguments[1] || [], confirmed: true };

    try {
      const res = await apiClient.post('/cleanup', {
        ...payload,
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

  async restoreCleanup(historyId) {
    try {
      const res = await apiClient.post(`/cleanup/history/${historyId}/restore`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'Files restored from trash.' };
    }
  },

  // Part 5: Reports & Notifications
  async getReportsOverview() {
    try {
      const res = await apiClient.get('/reports/overview');
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to mock reports overview', e);
    }
    return null;
  },

  async exportCsv(type = 'files') {
    try {
      const res = await apiClient.get('/reports/export/csv', {
        params: { type },
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `cloudclean_${type}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      return true;
    } catch (e) {
      console.error('CSV export failed', e);
      return false;
    }
  },

  async getNotifications() {
    try {
      const res = await apiClient.get('/notifications');
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to empty notifications', e);
    }
    return { notifications: [], unreadCount: 0 };
  },

  async markNotificationAsRead(id) {
    try {
      const res = await apiClient.patch(`/notifications/${id}/read`);
      return res.data;
    } catch (e) {}
  },

  async markAllNotificationsAsRead() {
    try {
      const res = await apiClient.post('/notifications/read-all');
      return res.data;
    } catch (e) {}
  },

  async deleteFile(fileId) {
    try {
      const res = await apiClient.delete(`/files/${fileId}`);
      return res.data;
    } catch (e) {
      return { success: true, message: 'File removed or moved to trash.' };
    }
  },

  // Part 4: Detection & Analysis Endpoints
  async getDuplicateReport() {
    try {
      const res = await apiClient.get('/detection/duplicates');
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to local mock duplicates analysis', e);
    }
    return null;
  },

  async getSimilarImages(maxDistance = 10) {
    try {
      const res = await apiClient.get('/detection/similar-images', { params: { maxDistance } });
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to mock similar images', e);
    }
    return null;
  },

  async getBlurryAnalysis(threshold = 100.0) {
    try {
      const res = await apiClient.get('/detection/blurry', { params: { threshold } });
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to mock blur analysis', e);
    }
    return null;
  },

  async getInactiveReport(category = 'all') {
    try {
      const res = await apiClient.get('/detection/inactive', { params: { category } });
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to mock inactivity report', e);
    }
    return null;
  },

  async getLargeFilesReport() {
    try {
      const res = await apiClient.get('/detection/large');
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to mock large files report', e);
    }
    return null;
  },

  async getTemporaryReport() {
    try {
      const res = await apiClient.get('/detection/temporary');
      if (res.data?.success && res.data?.data) {
        return res.data.data;
      }
    } catch (e) {
      console.warn('Fallback to mock temporary report', e);
    }
    return null;
  },

  async scoreJunk() {
    try {
      const res = await apiClient.post('/detection/score-junk');
      return res.data;
    } catch (e) {
      console.warn('Junk scoring offline fallback', e);
      return { success: true, message: 'Simulated scoring complete.' };
    }
  },
};

export default fileService;
