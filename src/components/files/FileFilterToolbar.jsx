import React from 'react';
import { Filter, SlidersHorizontal, RefreshCcw } from 'lucide-react';
import SearchInput from '../common/SearchInput';
import Button from '../common/Button';

export const FileFilterToolbar = ({
  search,
  onSearchChange,
  category,
  onCategoryChange,
  categories = [
    { id: 'all', label: 'All Files' },
    { id: 'duplicate', label: 'Duplicates' },
    { id: 'blurry', label: 'Blurry Photos' },
    { id: 'large', label: 'Large Files' },
    { id: 'inactive', label: 'Inactive' },
  ],
  sortBy,
  onSortByChange,
  totalResults = 0,
  onReset,
}) => {
  return (
    <div className="bg-white rounded-xl border border-slate-200/80 p-4 mb-4 shadow-sm space-y-3">
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Search */}
        <div className="w-full sm:w-80">
          <SearchInput
            value={search}
            onChange={onSearchChange}
            placeholder="Search by file name, path, hash..."
          />
        </div>

        {/* Sort and total badge */}
        <div className="flex items-center gap-3 justify-between sm:justify-end">
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-500 font-medium">Sort by:</span>
            <select
              value={sortBy}
              onChange={(e) => onSortByChange && onSortByChange(e.target.value)}
              className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="size">Size (Highest First)</option>
              <option value="name">Name (A-Z)</option>
              <option value="date">Modification Date</option>
              <option value="score">Detection Score</option>
            </select>
          </div>

          <span className="text-xs font-semibold px-2.5 py-1 rounded-md bg-slate-100 text-slate-600 whitespace-nowrap">
            {totalResults} items
          </span>
        </div>
      </div>

      {/* Category Pills */}
      {categories.length > 0 && (
        <div className="flex items-center gap-1.5 overflow-x-auto pt-2 border-t border-slate-100 text-xs">
          <span className="text-slate-400 mr-1 flex items-center gap-1 font-medium">
            <Filter className="w-3.5 h-3.5" />
            Category:
          </span>
          {categories.map((cat) => {
            const isSelected = category === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => onCategoryChange && onCategoryChange(cat.id)}
                className={`px-3 py-1 rounded-lg font-medium transition-all whitespace-nowrap ${
                  isSelected
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-slate-900 border border-slate-200/60'
                }`}
              >
                {cat.label}
              </button>
            );
          })}

          {(search || (category && category !== 'all')) && (
            <button
              onClick={onReset}
              className="ml-auto text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1 p-1"
            >
              <RefreshCcw className="w-3 h-3" />
              Reset
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default FileFilterToolbar;

