import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  LayoutDashboard,
  HardDrive,
  ScanSearch,
  Files,
  Copy,
  ImageOff,
  Maximize2,
  Clock,
  Sparkles,
  History,
  BarChart3,
  Settings,
  ArrowRight,
  Sparkle,
} from 'lucide-react';

const searchCommands = [
  { name: 'Dashboard', path: '/dashboard', category: 'Navigation', icon: LayoutDashboard },
  { name: 'Storage Connections', path: '/connections', category: 'Navigation', icon: HardDrive },
  { name: 'File Scanner & Diagnostics', path: '/scanner', category: 'Navigation', icon: ScanSearch },
  { name: 'All Files Catalog', path: '/files', category: 'Files', icon: Files },
  { name: 'Duplicate Files (1,482 detected)', path: '/files/duplicates', category: 'Files', icon: Copy },
  { name: 'Blurry Images (348 detected)', path: '/files/blurry', category: 'Files', icon: ImageOff },
  { name: 'Large Files >500MB', path: '/files/large', category: 'Files', icon: Maximize2 },
  { name: 'Inactive Files >180 Days', path: '/files/inactive', category: 'Files', icon: Clock },
  { name: 'Cleanup Recommendations', path: '/recommendations', category: 'Action', icon: Sparkles },
  { name: 'Cleanup History & Audit Logs', path: '/history', category: 'Audit', icon: History },
  { name: 'Reports & Cost Analytics', path: '/reports', category: 'Analytics', icon: BarChart3 },
  { name: 'User & Heuristic Settings', path: '/settings', category: 'Configuration', icon: Settings },
];

export const CommandPalette = ({ isOpen, onClose, onTriggerScan }) => {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        if (isOpen) {
          onClose();
        } else {
          // Open handled by parent listener
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const filteredCommands = searchCommands.filter((cmd) =>
    cmd.name.toLowerCase().includes(query.toLowerCase()) ||
    cmd.category.toLowerCase().includes(query.toLowerCase())
  );

  const handleSelect = (item) => {
    navigate(item.path);
    onClose();
    setQuery('');
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto p-4 sm:p-6 md:p-20">
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      <div className="relative mx-auto max-w-xl transform rounded-2xl bg-white shadow-2xl ring-1 ring-slate-900/5 transition-all overflow-hidden border border-slate-200">
        <div className="relative flex items-center px-4 py-3.5 border-b border-slate-100">
          <Search className="w-5 h-5 text-slate-400 mr-3 pointer-events-none" />
          <input
            type="text"
            autoFocus
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setSelectedIndex(0);
            }}
            placeholder="Type a page, command, or file filter... (Esc to exit)"
            className="w-full text-sm text-slate-800 placeholder-slate-400 bg-transparent focus:outline-none"
          />
          <kbd className="hidden sm:inline-block px-2 py-0.5 text-[10px] font-semibold text-slate-400 bg-slate-100 border border-slate-200 rounded">
            ESC
          </kbd>
        </div>

        {/* Results list */}
        <div className="max-h-80 overflow-y-auto py-2">
          {filteredCommands.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-400">
              No results found for "{query}"
            </div>
          ) : (
            filteredCommands.map((cmd, idx) => {
              const Icon = cmd.icon;
              return (
                <button
                  key={cmd.name}
                  onClick={() => handleSelect(cmd)}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs text-left hover:bg-slate-50 transition-colors group"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-1.5 rounded-lg bg-slate-100 text-slate-500 group-hover:bg-indigo-50 group-hover:text-indigo-600 transition-colors">
                      <Icon className="w-4 h-4" />
                    </div>
                    <div>
                      <p className="font-semibold text-slate-800 group-hover:text-indigo-600">
                        {cmd.name}
                      </p>
                      <span className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold">
                        {cmd.category}
                      </span>
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-slate-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                </button>
              );
            })
          )}
        </div>

        {/* Footer shortcuts */}
        <div className="px-4 py-2.5 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
          <div className="flex items-center gap-3">
            <span>
              <kbd className="font-mono bg-white px-1.5 py-0.5 rounded border border-slate-200">
                ↵
              </kbd>{' '}
              to select
            </span>
            <span>
              <kbd className="font-mono bg-white px-1.5 py-0.5 rounded border border-slate-200">
                esc
              </kbd>{' '}
              to close
            </span>
          </div>
          <span className="font-bold text-indigo-600">CloudClean AI Quick Search</span>
        </div>
      </div>
    </div>
  );
};

export default CommandPalette;

