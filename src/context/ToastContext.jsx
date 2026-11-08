import React, { createContext, useContext, useState, useCallback } from 'react';
import {
  CheckCircle2,
  AlertCircle,
  Info,
  AlertTriangle,
  X,
} from 'lucide-react';

const ToastContext = createContext(null);

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = useCallback(
    ({ type = 'info', title, message, duration = 4500, action }) => {
      const id = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`;
      const newToast = { id, type, title, message, action };

      setToasts((prev) => [...prev, newToast]);

      if (duration > 0) {
        setTimeout(() => {
          removeToast(id);
        }, duration);
      }
      return id;
    },
    [removeToast]
  );

  const toast = {
    success: (title, message, options) =>
      addToast({ type: 'success', title, message, ...options }),
    error: (title, message, options) =>
      addToast({ type: 'error', title, message, ...options }),
    info: (title, message, options) =>
      addToast({ type: 'info', title, message, ...options }),
    warning: (title, message, options) =>
      addToast({ type: 'warning', title, message, ...options }),
  };

  return (
    <ToastContext.Provider value={{ toast, addToast, removeToast }}>
      {children}
      {/* Toast Render Container */}
      <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none px-4 sm:px-0">
        {toasts.map((t) => {
          const typeConfig = {
            success: {
              icon: CheckCircle2,
              border: 'border-emerald-200',
              bg: 'bg-white',
              iconColor: 'text-emerald-500',
              badgeBg: 'bg-emerald-50',
            },
            error: {
              icon: AlertCircle,
              border: 'border-rose-200',
              bg: 'bg-white',
              iconColor: 'text-rose-500',
              badgeBg: 'bg-rose-50',
            },
            warning: {
              icon: AlertTriangle,
              border: 'border-amber-200',
              bg: 'bg-white',
              iconColor: 'text-amber-500',
              badgeBg: 'bg-amber-50',
            },
            info: {
              icon: Info,
              border: 'border-indigo-200',
              bg: 'bg-white',
              iconColor: 'text-indigo-500',
              badgeBg: 'bg-indigo-50',
            },
          }[t.type] || {
            icon: Info,
            border: 'border-slate-200',
            bg: 'bg-white',
            iconColor: 'text-slate-500',
            badgeBg: 'bg-slate-50',
          };

          const Icon = typeConfig.icon;

          return (
            <div
              key={t.id}
              className={`pointer-events-auto rounded-xl border ${typeConfig.border} ${typeConfig.bg} p-3.5 shadow-xl shadow-slate-900/10 flex items-start gap-3 transition-all duration-300 transform translate-y-0 opacity-100 animate-slide-up`}
            >
              <div
                className={`p-1.5 rounded-lg ${typeConfig.badgeBg} ${typeConfig.iconColor} shrink-0`}
              >
                <Icon className="w-4 h-4" />
              </div>
              <div className="flex-1 min-w-0">
                {t.title && (
                  <h5 className="text-xs font-bold text-slate-900 leading-tight">
                    {t.title}
                  </h5>
                )}
                {t.message && (
                  <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                    {t.message}
                  </p>
                )}
                {t.action && (
                  <div className="mt-2">
                    <button
                      onClick={() => {
                        t.action.onClick();
                        removeToast(t.id);
                      }}
                      className="text-xs font-bold text-indigo-600 hover:text-indigo-800 underline"
                    >
                      {t.action.label}
                    </button>
                  </div>
                )}
              </div>
              <button
                onClick={() => removeToast(t.id)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-md transition-colors"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

export default ToastContext;

