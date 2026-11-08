import React, { useState, useEffect } from 'react';
import PageHeader from '../components/layout/PageHeader';
import FileFilterToolbar from '../components/files/FileFilterToolbar';
import FileTable from '../components/files/FileTable';
import Pagination from '../components/common/Pagination';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import { fileService } from '../services/fileService';
import { Download, RefreshCw, Trash2, Archive } from 'lucide-react';

export const AllFiles = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('all');
  const [sortBy, setSortBy] = useState('size');
  const [sortOrder, setSortOrder] = useState('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, totalCount: 0, totalPages: 1 });

  const fetchFiles = async () => {
    setLoading(true);
    try {
      const res = await fileService.getFiles({
        category,
        search,
        sortBy,
        sortOrder,
        page: currentPage,
        limit: 10,
      });
      setFiles(res.data);
      setPagination(res.pagination);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFiles();
  }, [category, search, sortBy, sortOrder, currentPage]);

  const handleSortChange = (newSortBy, newSortOrder) => {
    setSortBy(newSortBy);
    setSortOrder(newSortOrder);
    setCurrentPage(1);
  };

  const handleDeleteFile = async (fileId) => {
    await fileService.deleteFile(fileId);
    setFiles((prev) => prev.filter((f) => f.id !== fileId));
  };

  const handleArchiveFile = async (fileId) => {
    setFiles((prev) => prev.filter((f) => f.id !== fileId));
  };

  const handleResetFilters = () => {
    setSearch('');
    setCategory('all');
    setSortBy('size');
    setSortOrder('desc');
    setCurrentPage(1);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="All Indexed Files"
        subtitle="Complete catalog of files analyzed across local drives and cloud repositories with AI flags."
        breadcrumbs={[{ label: 'All Files' }]}
        actions={
          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCw}
            onClick={fetchFiles}
            isLoading={loading}
          >
            Refresh Catalog
          </Button>
        }
      />

      {/* Filter Toolbar */}
      <FileFilterToolbar
        search={search}
        onSearchChange={(q) => {
          setSearch(q);
          setCurrentPage(1);
        }}
        category={category}
        onCategoryChange={(c) => {
          setCategory(c);
          setCurrentPage(1);
        }}
        sortBy={sortBy}
        onSortByChange={(s) => {
          setSortBy(s);
          setCurrentPage(1);
        }}
        totalResults={pagination.totalCount}
        onReset={handleResetFilters}
      />

      {/* Main Professional File Table */}
      <FileTable
        files={files}
        loading={loading}
        sortBy={sortBy}
        sortOrder={sortOrder}
        onSortChange={handleSortChange}
        onDeleteFile={handleDeleteFile}
        onArchiveFile={handleArchiveFile}
      />

      {/* Pagination */}
      <Pagination
        currentPage={pagination.page}
        totalPages={pagination.totalPages}
        totalItems={pagination.totalCount}
        pageSize={pagination.limit}
        onPageChange={(page) => setCurrentPage(page)}
      />
    </div>
  );
};

export default AllFiles;

