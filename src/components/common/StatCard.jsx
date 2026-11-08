import React from 'react';
import Card from './Card';
import Badge from './Badge';

export const StatCard = ({
  title,
  value,
  subvalue,
  icon: Icon,
  badgeText,
  badgeVariant = 'primary',
  color = 'indigo', // 'indigo' | 'violet' | 'blue' | 'emerald' | 'amber' | 'rose'
  trend,
  trendType = 'neutral', // 'up' | 'down' | 'neutral'
  className = '',
  onClick,
}) => {
  const iconColorMap = {
    indigo: 'bg-indigo-50 text-indigo-600 border-indigo-100',
    violet: 'bg-violet-50 text-violet-600 border-violet-100',
    blue: 'bg-blue-50 text-blue-600 border-blue-100',
    emerald: 'bg-emerald-50 text-emerald-600 border-emerald-100',
    amber: 'bg-amber-50 text-amber-600 border-amber-100',
    rose: 'bg-rose-50 text-rose-600 border-rose-100',
  };

  return (
    <Card
      className={`relative overflow-hidden transition-all duration-200 ${
        onClick ? 'cursor-pointer hover:-translate-y-0.5 hover:shadow-md' : ''
      } ${className}`}
      onClick={onClick}
    >
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            {title}
          </p>
          <div className="flex items-baseline gap-2">
            <h4 className="text-2xl font-bold text-slate-900 tracking-tight">
              {value}
            </h4>
            {badgeText && (
              <Badge variant={badgeVariant} size="xs">
                {badgeText}
              </Badge>
            )}
          </div>
          {subvalue && (
            <p className="text-xs text-slate-500 font-medium">{subvalue}</p>
          )}
          {trend && (
            <div className="flex items-center gap-1 text-xs font-medium pt-1">
              <span
                className={
                  trendType === 'up'
                    ? 'text-emerald-600'
                    : trendType === 'down'
                    ? 'text-rose-600'
                    : 'text-slate-500'
                }
              >
                {trend}
              </span>
            </div>
          )}
        </div>

        {Icon && (
          <div
            className={`p-3 rounded-xl border ${
              iconColorMap[color] || iconColorMap.indigo
            } shadow-subtle`}
          >
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>
    </Card>
  );
};

export default StatCard;

