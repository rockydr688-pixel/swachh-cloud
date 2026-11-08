import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
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
  Sparkle,
  X,
  Database,
  ShieldCheck,
} from 'lucide-react';

const navigationGroups = [
  {
    title: 'OVERVIEW',
    items: [
      { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
      { name: 'Storage Connections', path: '/connections', icon: HardDrive },
      { name: 'File Scanner', path: '/scanner', icon: ScanSearch },
    ],
  },
  {
    title: 'OPTIMIZATION FILTERS',
    items: [
      { name: 'All Files', path: '/files', icon: Files },
      { name: 'Duplicate Files', path: '/files/duplicates', icon: Copy, badge: '1.4k' },
      { name: 'Blurry Images', path: '/files/blurry', icon: ImageOff, badge: '348' },
      { name: 'Large Files', path: '/files/large', icon: Maximize2, badge: '112 GB' },
      { name: 'Inactive Files', path: '/files/inactive', icon: Clock, badge: '620' },
    ],
  },
  {
    title: 'EXECUTION & ANALYTICS',
    items: [
      { name: 'Recommendations', path: '/recommendations', icon: Sparkles, highlight: true },
      { name: 'Cleanup History', path: '/history', icon: History },
      { name: 'Reports & Analytics', path: '/reports', icon: BarChart3 },
    ],
  },
  {
    title: 'SYSTEM',
    items: [
      { name: 'User Settings', path: '/settings', icon: Settings },
    ],
  },
];

export const Sidebar = ({ mobileOpen, setMobileOpen }) => {
  const location = useLocation();

  const sidebarContent = (
    <div className="flex flex-col h-full bg-[#0B132B] text-slate-300 border-r border-slate-800/80 select-none">
      {/* Brand Header */}
      <div className="flex items-center justify-between px-6 py-5 border-b border-slate-800/80">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-violet-500 p-0.5 shadow-lg shadow-indigo-500/20 flex items-center justify-center">
            <div className="w-full h-full bg-[#0B132B] rounded-[10px] flex items-center justify-center">
              <Sparkle className="w-5 h-5 text-indigo-400 fill-indigo-400/20" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-base text-white tracking-tight">
                CloudClean
              </span>
              <span className="px-1.5 py-0.5 text-[10px] font-extrabold uppercase bg-gradient-to-r from-blue-500 to-violet-500 text-white rounded">
                AI
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-medium">Storage Intelligence</p>
          </div>
        </div>

        {/* Mobile close button */}
        {setMobileOpen && (
          <button
            onClick={() => setMobileOpen(false)}
            className="md:hidden text-slate-400 hover:text-white p-1 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-6 overflow-y-auto">
        {navigationGroups.map((group) => (
          <div key={group.title} className="space-y-1">
            <h4 className="px-3 text-[10px] font-bold tracking-wider text-slate-400 uppercase">
              {group.title}
            </h4>
            <div className="space-y-0.5 pt-1">
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive =
                  location.pathname === item.path ||
                  (item.path !== '/dashboard' &&
                    location.pathname.startsWith(item.path) &&
                    item.path !== '/files');

                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    onClick={() => setMobileOpen && setMobileOpen(false)}
                    className={({ isActive: exactActive }) => {
                      const active = item.path === '/files' ? exactActive : isActive;
                      return `flex items-center justify-between px-3 py-2 text-xs font-medium rounded-lg transition-all duration-150 group ${
                        active
                          ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/20 font-semibold'
                          : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
                      }`;
                    }}
                  >
                    <div className="flex items-center gap-2.5">
                      <Icon
                        className={`w-4 h-4 transition-colors ${
                          isActive
                            ? 'text-white'
                            : item.highlight
                            ? 'text-violet-400 group-hover:text-violet-300'
                            : 'text-slate-400 group-hover:text-slate-200'
                        }`}
                      />
                      <span>{item.name}</span>
                    </div>

                    {item.badge && (
                      <span
                        className={`text-[10px] px-1.5 py-0.2 rounded font-semibold ${
                          isActive
                            ? 'bg-white/20 text-white'
                            : 'bg-slate-800 text-slate-300 border border-slate-700/60'
                        }`}
                      >
                        {item.badge}
                      </span>
                    )}

                    {item.highlight && !isActive && (
                      <span className="w-1.5 h-1.5 rounded-full bg-violet-400 animate-pulse" />
                    )}
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* Storage Agent Status & Sample Indicator */}
      <div className="p-4 border-t border-slate-800/80 bg-[#070D1F]/70">
        <div className="p-3 rounded-xl bg-slate-900/90 border border-slate-800">
          <div className="flex items-center justify-between text-xs mb-1.5">
            <span className="text-slate-400 font-medium flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              Agent IPC
            </span>
            <span className="text-[11px] font-semibold text-emerald-400">Ready</span>
          </div>
          <p className="text-[11px] text-slate-400 leading-tight">
            Local daemon indexing active. Google Drive integration awaiting OAuth.
          </p>
        </div>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop static sidebar */}
      <aside className="hidden md:flex md:w-64 md:flex-col md:fixed md:inset-y-0 z-30">
        {sidebarContent}
      </aside>

      {/* Mobile drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setMobileOpen(false)}
          />
          <div className="relative flex-1 flex flex-col max-w-xs w-full bg-[#0B132B]">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
};

export default Sidebar;

