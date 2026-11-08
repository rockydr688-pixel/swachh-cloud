import React from 'react';

export const ProgressBar = ({
  value = 0,
  max = 100,
  color = 'indigo', // 'indigo' | 'violet' | 'blue' | 'emerald' | 'amber' | 'rose'
  size = 'md', // 'sm' | 'md' | 'lg'
  showLabel = false,
  label,
  className = '',
}) => {
  const percentage = Math.min(Math.max(Math.round((value / max) * 100), 0), 100);

  const colorClasses = {
    indigo: 'bg-indigo-600',
    violet: 'bg-violet-600',
    blue: 'bg-blue-600',
    emerald: 'bg-emerald-500',
    amber: 'bg-amber-500',
    rose: 'bg-rose-500',
    gradient: 'bg-gradient-to-r from-blue-600 via-indigo-600 to-violet-600',
  };

  const sizeClasses = {
    sm: 'h-1.5',
    md: 'h-2.5',
    lg: 'h-4',
  };

  return (
    <div className={`w-full ${className}`}>
      {(showLabel || label) && (
        <div className="flex items-center justify-between text-xs text-slate-600 mb-1.5 font-medium">
          <span>{label}</span>
          <span className="font-semibold text-slate-800">{percentage}%</span>
        </div>
      )}
      <div
        className={`w-full bg-slate-100 rounded-full overflow-hidden ${
          sizeClasses[size] || sizeClasses.md
        }`}
      >
        <div
          className={`h-full transition-all duration-500 rounded-full ${
            colorClasses[color] || colorClasses.indigo
          }`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
};

export default ProgressBar;

