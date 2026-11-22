import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  Home,
  HardDrive,
  Sparkles,
  Clock,
  Settings,
  X,
  ShieldCheck,
  Cloud,
} from 'lucide-react';
import { useStorage } from '../../context/StorageContext';
import { formatBytes } from '../../utils/formatters';

// Exactly 5 main navigation items per Section 2
const mainNavigation = [
  { name: 'Home', path: '/dashboard', icon: Home },
  { name: 'My Storage', path: '/storage', icon: HardDrive },
  { name: 'Junk Files', path: '/junk', icon: Sparkles, badge: true },
  { name: 'Scan History', path: '/history', icon: Clock },
  { name: 'Settings', path: '/settings', icon: Settings },
];

export const Sidebar = ({ mobileOpen, setMobileOpen }) => {
  const { stats, recommendations } = useStorage();

  const recoverableFormatted = stats?.recoverableBytes
    ? formatBytes(stats.recoverableBytes)
    : recommendations?.length > 0
    ? `${recommendations.length} files`
    : null;

  const sidebarContent = (
    <div className="flex flex-col h-full bg-white text-slate-700 border-r border-slate-200/90 select-none">
      {/* Brand Header */}
      <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
        <NavLink to="/dashboard" className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center shadow-sm">
            <Cloud className="w-4 h-4 fill-white/20" />
          </div>
          <div className="flex items-center gap-1.5">
            <span className="font-bold text-base text-slate-900 tracking-tight">
              CloudClean
            </span>
            <span className="text-[10px] font-bold uppercase px-1.5 py-0.5 rounded bg-blue-50 text-blue-600">
              AI
            </span>
          </div>
        </NavLink>

        {/* Mobile close button */}
        {setMobileOpen && (
          <button
            onClick={() => setMobileOpen(false)}
            className="md:hidden text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-50 transition-colors"
            aria-label="Close menu"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* 5 Main Navigation Items */}
      <nav className="flex-1 px-3 py-5 space-y-1 overflow-y-auto">
        {mainNavigation.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.name}
              to={item.path}
              onClick={() => setMobileOpen && setMobileOpen(false)}
              className={({ isActive }) =>
                `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-blue-50 text-blue-700 font-semibold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`
              }
            >
              <div className="flex items-center gap-3">
                <Icon className="w-4 h-4 shrink-0" />
                <span>{item.name}</span>
              </div>

              {item.badge && recoverableFormatted && (
                <span className="px-2 py-0.5 text-[11px] font-semibold rounded-full bg-emerald-50 text-emerald-700 border border-emerald-100">
                  {recoverableFormatted}
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* Storage Status Footer Widget */}
      <div className="p-4 m-3 rounded-xl bg-slate-50 border border-slate-100">
        <div className="flex items-center justify-between text-xs text-slate-500 mb-1.5">
          <span>Storage Status</span>
          <span className="font-semibold text-slate-700">
            {stats ? `${Math.round((stats.totalUsedBytes / (stats.totalCapacityBytes || 1)) * 100)}% Used` : 'Protected'}
          </span>
        </div>
        <div className="w-full h-1.5 bg-slate-200 rounded-full overflow-hidden">
          <div
            className="h-full bg-blue-600 rounded-full transition-all duration-300"
            style={{
              width: `${Math.min(100, Math.round((stats?.totalUsedBytes / (stats?.totalCapacityBytes || 1)) * 100) || 45)}%`,
            }}
          />
        </div>
        <div className="mt-2 text-[11px] text-slate-500 flex items-center gap-1.5">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
          <span>Encrypted & Safe</span>
        </div>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop fixed sidebar */}
      <aside className="hidden md:flex md:w-60 md:flex-col md:fixed md:inset-y-0 z-30">
        {sidebarContent}
      </aside>

      {/* Mobile drawer backdrop */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/30 backdrop-blur-sm md:hidden animate-fade-in"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Mobile drawer */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-white transform transition-transform duration-200 ease-in-out md:hidden shadow-2xl ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {sidebarContent}
      </aside>
    </>
  );
};

export default Sidebar;
