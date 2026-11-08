import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Menu,
  Bell,
  Search,
  Sparkles,
  User,
  LogOut,
  Settings,
  Shield,
  HelpCircle,
  Database,
  CheckCircle2,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useStorage } from '../../context/StorageContext';
import Button from '../common/Button';
import Badge from '../common/Badge';

export const Navbar = ({ setMobileOpen }) => {
  const { user, logout } = useAuth();
  const { stats, isScanning, runScan } = useStorage();
  const navigate = useNavigate();
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const dropdownRef = useRef(null);
  const notifRef = useRef(null);

  // Close dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setProfileDropdownOpen(false);
      }
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setNotificationsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/auth');
  };

  return (
    <header className="sticky top-0 z-20 bg-white/95 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 py-3">
      <div className="flex items-center justify-between gap-4">
        {/* Left: Mobile Toggle & Quick Search */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setMobileOpen(true)}
            className="md:hidden p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 focus:outline-none"
          >
            <Menu className="w-5 h-5" />
          </button>

          {/* Quick Search */}
          <div className="relative hidden sm:block w-64 lg:w-80">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
            <input
              type="text"
              placeholder="Search files, recommendations, logs... (Ctrl+K)"
              onClick={() => navigate('/files')}
              readOnly
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg text-slate-600 placeholder-slate-400 cursor-pointer hover:bg-slate-100 transition-colors focus:outline-none"
            />
          </div>
        </div>

        {/* Center/Right: Sample Data Banner & Actions */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Sample Data Indicator */}
          <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-50 border border-amber-200/80 text-[11px] font-semibold text-amber-800">
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
            <span>Sample Data Environment</span>
          </div>

          {/* Scan Action */}
          <Button
            size="sm"
            variant="primary"
            icon={Sparkles}
            isLoading={isScanning}
            onClick={() => runScan('Quick Scan')}
            className="text-xs bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-sm"
          >
            {isScanning ? 'Analyzing...' : 'Scan Storage'}
          </Button>

          {/* Notifications Dropdown */}
          <div className="relative" ref={notifRef}>
            <button
              onClick={() => setNotificationsOpen(!notificationsOpen)}
              className="relative p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
            >
              <Bell className="w-4 h-4" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-indigo-600 rounded-full ring-2 ring-white" />
            </button>

            {notificationsOpen && (
              <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-100 py-2 z-50 animate-scale-up">
                <div className="px-4 py-2 border-b border-slate-100 flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-800 uppercase tracking-wide">
                    Notifications
                  </span>
                  <span className="text-[10px] text-indigo-600 font-semibold bg-indigo-50 px-2 py-0.5 rounded-full">
                    3 New
                  </span>
                </div>
                <div className="divide-y divide-slate-50 max-h-64 overflow-y-auto">
                  <div className="p-3 hover:bg-slate-50 transition-colors">
                    <p className="text-xs font-semibold text-slate-800">
                      High Recoverable Space Detected
                    </p>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      1.4k duplicates found across Local Downloads. ~89.2 GB safe to delete.
                    </p>
                    <span className="text-[10px] text-slate-400 mt-1 block">15m ago</span>
                  </div>
                  <div className="p-3 hover:bg-slate-50 transition-colors">
                    <p className="text-xs font-semibold text-slate-800">
                      Weekly Cloud Scan Completed
                    </p>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      42,850 files indexed in 11 minutes.
                    </p>
                    <span className="text-[10px] text-slate-400 mt-1 block">2h ago</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* User Profile Menu */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
              className="flex items-center gap-2 p-1 pl-2 rounded-lg hover:bg-slate-100 transition-colors border border-transparent hover:border-slate-200"
            >
              <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-indigo-600 to-violet-600 text-white flex items-center justify-center font-bold text-xs shadow-sm">
                {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="hidden xl:block text-left text-xs leading-none">
                <p className="font-semibold text-slate-800">{user?.name || 'Administrator'}</p>
                <p className="text-[10px] text-slate-400 mt-0.5">{user?.email || 'admin@cloudclean.ai'}</p>
              </div>
            </button>

            {profileDropdownOpen && (
              <div className="absolute right-0 mt-2 w-56 bg-white rounded-xl shadow-xl border border-slate-100 py-1.5 z-50 animate-scale-up">
                <div className="px-4 py-2.5 border-b border-slate-100">
                  <p className="text-xs font-bold text-slate-900">{user?.name || 'Administrator'}</p>
                  <p className="text-[11px] text-slate-500 truncate">{user?.email || 'admin@cloudclean.ai'}</p>
                  <div className="mt-1.5 flex items-center gap-1">
                    <Badge variant="navy" size="xs">
                      Enterprise Tier
                    </Badge>
                  </div>
                </div>

                <div className="py-1">
                  <button
                    onClick={() => {
                      setProfileDropdownOpen(false);
                      navigate('/settings');
                    }}
                    className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 transition-colors"
                  >
                    <Settings className="w-3.5 h-3.5 text-slate-400" />
                    <span>Settings & Preferences</span>
                  </button>
                  <button
                    onClick={() => {
                      setProfileDropdownOpen(false);
                      navigate('/connections');
                    }}
                    className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 transition-colors"
                  >
                    <Database className="w-3.5 h-3.5 text-slate-400" />
                    <span>Storage Connections</span>
                  </button>
                </div>

                <div className="border-t border-slate-100 pt-1">
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-rose-600 hover:bg-rose-50 transition-colors font-medium"
                  >
                    <LogOut className="w-3.5 h-3.5 text-rose-500" />
                    <span>Sign Out</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Navbar;

