import React, { useState, useRef, useEffect, useCallback } from 'react';
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
  AlertCircle,
  Info,
  CheckCheck,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useStorage } from '../../context/StorageContext';
import { fileService } from '../../services/fileService';
import Button from '../common/Button';
import Badge from '../common/Badge';

export const Navbar = ({ setMobileOpen }) => {
  const { user, logout } = useAuth();
  const { stats, isScanning, runScan } = useStorage();
  const navigate = useNavigate();
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoadingNotifs, setIsLoadingNotifs] = useState(false);
  const dropdownRef = useRef(null);
  const notifRef = useRef(null);

  const fetchNotifications = useCallback(async () => {
    try {
      setIsLoadingNotifs(true);
      const data = await fileService.getNotifications();
      if (data && Array.isArray(data.notifications) && data.notifications.length > 0) {
        setNotifications(data.notifications);
        setUnreadCount(typeof data.unreadCount === 'number' ? data.unreadCount : 0);
      } else {
        // Fallback default notifications if none in db yet
        setNotifications([
          {
            id: 'mock-1',
            title: 'High Recoverable Space Detected',
            message: 'Duplicate & blurry file groups identified. Review recommendations to reclaim space.',
            type: 'new_recommendations',
            actionUrl: '/recommendations',
            createdAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
            isRead: false,
          },
          {
            id: 'mock-2',
            title: 'Storage Provider Ready',
            message: 'Local agent & Google Drive connections active.',
            type: 'scan_completed',
            actionUrl: '/connections',
            createdAt: new Date(Date.now() - 2 * 3600 * 1000).toISOString(),
            isRead: true,
          },
        ]);
        setUnreadCount(1);
      }
    } catch (e) {
      console.warn('Could not fetch notifications', e);
    } finally {
      setIsLoadingNotifs(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  // Refresh notifications when scan finishes
  useEffect(() => {
    if (!isScanning) {
      fetchNotifications();
    }
  }, [isScanning, fetchNotifications]);

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

  const handleMarkAllAsRead = async () => {
    try {
      await fileService.markAllNotificationsAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (e) {
      console.error(e);
    }
  };

  const handleNotificationClick = async (notif) => {
    if (!notif.isRead && notif.id && !notif.id.startsWith('mock-')) {
      try {
        await fileService.markNotificationAsRead(notif.id);
      } catch (e) {}
    }
    setNotifications((prev) =>
      prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n))
    );
    setUnreadCount((prev) => Math.max(0, prev - (notif.isRead ? 0 : 1)));
    setNotificationsOpen(false);
    if (notif.actionUrl) {
      navigate(notif.actionUrl);
    }
  };

  const formatRelativeTime = (isoString) => {
    if (!isoString) return 'Just now';
    const diff = Math.floor((new Date() - new Date(isoString)) / 1000);
    if (diff < 60) return 'Just now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

  const getNotifIcon = (type) => {
    switch (type) {
      case 'scan_completed':
      case 'cleanup_completed':
        return <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 mt-0.5 shrink-0" />;
      case 'scan_failed':
      case 'cleanup_failed':
        return <AlertCircle className="w-3.5 h-3.5 text-rose-500 mt-0.5 shrink-0" />;
      case 'new_recommendations':
        return <Sparkles className="w-3.5 h-3.5 text-indigo-500 mt-0.5 shrink-0" />;
      default:
        return <Info className="w-3.5 h-3.5 text-blue-500 mt-0.5 shrink-0" />;
    }
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
          {/* MongoDB Connection Status Indicator */}
          <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 border border-emerald-200/80 text-[11px] font-semibold text-emerald-800">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span>MongoDB Connected</span>
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
              title="Notifications"
            >
              <Bell className="w-4 h-4" />
              {unreadCount > 0 && (
                <span className="absolute top-1 right-1 flex items-center justify-center min-w-[16px] h-4 px-1 text-[10px] font-bold text-white bg-indigo-600 rounded-full ring-2 ring-white">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </button>

            {notificationsOpen && (
              <div className="absolute right-0 mt-2 w-84 sm:w-96 bg-white rounded-xl shadow-xl border border-slate-100 py-2 z-50 animate-scale-up">
                <div className="px-4 py-2.5 border-b border-slate-100 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-slate-800 uppercase tracking-wide">
                      Notifications
                    </span>
                    {unreadCount > 0 && (
                      <span className="text-[10px] text-indigo-600 font-semibold bg-indigo-50 px-2 py-0.5 rounded-full">
                        {unreadCount} Unread
                      </span>
                    )}
                  </div>
                  {unreadCount > 0 && (
                    <button
                      onClick={handleMarkAllAsRead}
                      className="text-[11px] text-slate-500 hover:text-indigo-600 flex items-center gap-1 font-medium transition-colors"
                      title="Mark all as read"
                    >
                      <CheckCheck className="w-3.5 h-3.5" />
                      Mark all read
                    </button>
                  )}
                </div>

                <div className="divide-y divide-slate-50 max-h-72 overflow-y-auto">
                  {notifications.length === 0 ? (
                    <div className="py-8 text-center text-slate-400">
                      <Bell className="w-6 h-6 mx-auto mb-1.5 opacity-40" />
                      <p className="text-xs font-medium">All caught up!</p>
                      <p className="text-[11px] text-slate-400">No new notifications right now.</p>
                    </div>
                  ) : (
                    notifications.map((notif) => (
                      <div
                        key={notif.id || notif._id}
                        onClick={() => handleNotificationClick(notif)}
                        className={`p-3 transition-colors cursor-pointer flex items-start gap-2.5 ${
                          notif.isRead
                            ? 'hover:bg-slate-50 opacity-80'
                            : 'bg-indigo-50/30 hover:bg-indigo-50/50'
                        }`}
                      >
                        {getNotifIcon(notif.type)}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between gap-1">
                            <p
                              className={`text-xs truncate ${
                                notif.isRead
                                  ? 'font-medium text-slate-700'
                                  : 'font-semibold text-slate-900'
                              }`}
                            >
                              {notif.title}
                            </p>
                            {!notif.isRead && (
                              <span className="w-1.5 h-1.5 rounded-full bg-indigo-600 shrink-0" />
                            )}
                          </div>
                          <p className="text-[11px] text-slate-500 mt-0.5 line-clamp-2 leading-relaxed">
                            {notif.message}
                          </p>
                          <span className="text-[10px] text-slate-400 mt-1 block">
                            {formatRelativeTime(notif.createdAt)}
                          </span>
                        </div>
                      </div>
                    ))
                  )}
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

