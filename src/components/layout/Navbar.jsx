import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Menu,
  Bell,
  Search,
  Sparkles,
  LogOut,
  Settings,
  HardDrive,
  CheckCircle2,
  AlertCircle,
  Info,
  CheckCheck,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useStorage } from '../../context/StorageContext';
import { fileService } from '../../services/fileService';
import Button from '../common/Button';
import ScanModal from '../scanner/ScanModal';

export const Navbar = ({ setMobileOpen }) => {
  const { user, logout } = useAuth();
  const { stats, isScanning } = useStorage();
  const navigate = useNavigate();

  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [scanModalOpen, setScanModalOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const dropdownRef = useRef(null);
  const notifRef = useRef(null);

  const fetchNotifications = useCallback(async () => {
    try {
      const data = await fileService.getNotifications();
      if (data && Array.isArray(data.notifications) && data.notifications.length > 0) {
        setNotifications(data.notifications);
        setUnreadCount(typeof data.unreadCount === 'number' ? data.unreadCount : 0);
      } else {
        setNotifications([
          {
            id: 'default-notif-1',
            title: 'Welcome to CloudClean AI',
            message: 'Connect your cloud accounts and scan for duplicate or unnecessary files.',
            type: 'info',
            actionUrl: '/storage',
            createdAt: new Date().toISOString(),
            isRead: false,
          },
        ]);
        setUnreadCount(1);
      }
    } catch (e) {
      console.warn('Could not fetch notifications', e);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  useEffect(() => {
    if (!isScanning) {
      fetchNotifications();
    }
  }, [isScanning, fetchNotifications]);

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
    if (!notif.isRead && notif.id && !notif.id.startsWith('default-')) {
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

  const getNotifIcon = (type) => {
    switch (type) {
      case 'scan_completed':
      case 'cleanup_completed':
        return <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 mt-0.5 shrink-0" />;
      case 'scan_failed':
      case 'cleanup_failed':
        return <AlertCircle className="w-3.5 h-3.5 text-rose-500 mt-0.5 shrink-0" />;
      default:
        return <Info className="w-3.5 h-3.5 text-blue-500 mt-0.5 shrink-0" />;
    }
  };

  return (
    <>
      <header className="sticky top-0 z-20 bg-white/95 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 py-3">
        <div className="flex items-center justify-between gap-4">
          {/* Left: Mobile Drawer Button & Quick Search */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMobileOpen(true)}
              className="md:hidden p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 focus:outline-none transition-colors"
              aria-label="Open navigation menu"
            >
              <Menu className="w-5 h-5" />
            </button>

            {/* Quick Search */}
            <div className="relative hidden sm:block w-64 lg:w-80">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
              <input
                type="text"
                placeholder="Search files, duplicates, photos..."
                onClick={() => navigate('/junk')}
                readOnly
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg text-slate-600 placeholder-slate-400 cursor-pointer hover:bg-slate-100 transition-colors focus:outline-none"
              />
            </div>
          </div>

          {/* Right: Actions */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Primary Action Button: Scan My Storage */}
            <Button
              size="sm"
              variant="primary"
              icon={Sparkles}
              isLoading={isScanning}
              onClick={() => setScanModalOpen(true)}
              className="text-xs bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-sm"
            >
              {isScanning ? 'Scanning...' : 'Scan My Storage'}
            </Button>

            {/* Notifications Dropdown */}
            <div className="relative" ref={notifRef}>
              <button
                onClick={() => setNotificationsOpen(!notificationsOpen)}
                className="relative p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
                title="Notifications"
                aria-label="Notifications"
              >
                <Bell className="w-4 h-4" />
                {unreadCount > 0 && (
                  <span className="absolute top-1 right-1 flex items-center justify-center min-w-[16px] h-4 px-1 text-[10px] font-bold text-white bg-blue-600 rounded-full ring-2 ring-white">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {notificationsOpen && (
                <div className="absolute right-0 mt-2 w-80 sm:w-88 bg-white rounded-xl shadow-xl border border-slate-100 py-2 z-50 animate-scale-up">
                  <div className="px-4 py-2.5 border-b border-slate-100 flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-800 uppercase tracking-wide">
                      Notifications
                    </span>
                    {unreadCount > 0 && (
                      <button
                        onClick={handleMarkAllAsRead}
                        className="text-[11px] text-slate-500 hover:text-blue-600 flex items-center gap-1 font-medium transition-colors"
                      >
                        <CheckCheck className="w-3.5 h-3.5" />
                        Mark all read
                      </button>
                    )}
                  </div>

                  <div className="divide-y divide-slate-50 max-h-72 overflow-y-auto">
                    {notifications.length === 0 ? (
                      <div className="py-8 text-center text-slate-400">
                        <Bell className="w-5 h-5 mx-auto mb-1.5 opacity-40" />
                        <p className="text-xs font-medium">All caught up!</p>
                      </div>
                    ) : (
                      notifications.map((notif) => (
                        <div
                          key={notif.id || notif._id}
                          onClick={() => handleNotificationClick(notif)}
                          className={`p-3 transition-colors cursor-pointer flex items-start gap-2.5 ${
                            notif.isRead
                              ? 'hover:bg-slate-50 opacity-80'
                              : 'bg-blue-50/30 hover:bg-blue-50/50'
                          }`}
                        >
                          {getNotifIcon(notif.type)}
                          <div className="flex-1 min-w-0">
                            <p
                              className={`text-xs truncate ${
                                notif.isRead
                                  ? 'font-medium text-slate-700'
                                  : 'font-semibold text-slate-900'
                              }`}
                            >
                              {notif.title}
                            </p>
                            <p className="text-[11px] text-slate-500 mt-0.5 line-clamp-2 leading-relaxed">
                              {notif.message}
                            </p>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Profile Dropdown */}
            <div className="relative" ref={dropdownRef}>
              <button
                onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                className="flex items-center gap-2 p-1 pl-2 rounded-lg hover:bg-slate-100 transition-colors"
                aria-label="User profile menu"
              >
                <div className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-sm">
                  {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <div className="hidden xl:block text-left text-xs leading-none">
                  <p className="font-semibold text-slate-800">{user?.name || 'User'}</p>
                  <p className="text-[10px] text-slate-400 mt-0.5">{user?.email || 'user@example.com'}</p>
                </div>
              </button>

              {profileDropdownOpen && (
                <div className="absolute right-0 mt-2 w-52 bg-white rounded-xl shadow-xl border border-slate-100 py-1.5 z-50 animate-scale-up">
                  <div className="px-4 py-2.5 border-b border-slate-100">
                    <p className="text-xs font-bold text-slate-900">{user?.name || 'User'}</p>
                    <p className="text-[11px] text-slate-500 truncate">{user?.email || 'user@example.com'}</p>
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
                      <span>Settings</span>
                    </button>
                    <button
                      onClick={() => {
                        setProfileDropdownOpen(false);
                        navigate('/storage');
                      }}
                      className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 transition-colors"
                    >
                      <HardDrive className="w-3.5 h-3.5 text-slate-400" />
                      <span>My Storage</span>
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

      {/* Global Scan Modal */}
      <ScanModal
        isOpen={scanModalOpen}
        onClose={() => setScanModalOpen(false)}
      />
    </>
  );
};

export default Navbar;
