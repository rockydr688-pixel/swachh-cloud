import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { FileFilterToolbar } from '../components/files/FileFilterToolbar';

describe('File Filtering and Toolbar Component Tests', () => {
  it('renders search input, category filters, and sort options', () => {
    const handleSearchChange = vi.fn();
    const handleCategoryChange = vi.fn();
    const handleSortChange = vi.fn();

    render(
      <FileFilterToolbar
        search="presentation"
        onSearchChange={handleSearchChange}
        category="all"
        onCategoryChange={handleCategoryChange}
        sortBy="size"
        onSortByChange={handleSortChange}
        totalResults={42}
      />
    );

    // Check search input value
    const searchInput = screen.getByPlaceholderText('Search by file name, path, hash...');
    expect(searchInput.value).toBe('presentation');

    // Check total results badge
    expect(screen.getByText('42 items')).toBeDefined();

    // Check categories
    expect(screen.getByText('All Files')).toBeDefined();
    expect(screen.getByText('Duplicates')).toBeDefined();
    expect(screen.getByText('Blurry Photos')).toBeDefined();
    expect(screen.getByText('Large Files')).toBeDefined();
    expect(screen.getByText('Inactive')).toBeDefined();
  });

  it('triggers category change when pill is clicked', () => {
    const handleCategoryChange = vi.fn();

    render(
      <FileFilterToolbar
        search=""
        onSearchChange={vi.fn()}
        category="all"
        onCategoryChange={handleCategoryChange}
        sortBy="size"
        onSortByChange={vi.fn()}
        totalResults={10}
      />
    );

    const duplicatePill = screen.getByText('Duplicates');
    fireEvent.click(duplicatePill);

    expect(handleCategoryChange).toHaveBeenCalledWith('duplicate');
  });

  it('triggers sort order change when dropdown is changed', () => {
    const handleSortChange = vi.fn();

    render(
      <FileFilterToolbar
        search=""
        onSearchChange={vi.fn()}
        category="all"
        onCategoryChange={vi.fn()}
        sortBy="size"
        onSortByChange={handleSortChange}
        totalResults={10}
      />
    );

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'name' } });

    expect(handleSortChange).toHaveBeenCalledWith('name');
  });
});
