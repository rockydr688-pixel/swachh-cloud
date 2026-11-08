import React from 'react';

export const Card = ({
  children,
  className = '',
  title,
  subtitle,
  headerAction,
  footer,
  noPadding = false,
  hoverEffect = false,
}) => {
  return (
    <div
      className={`bg-white rounded-xl border border-slate-200/80 shadow-sm transition-all duration-200 ${
        hoverEffect ? 'hover:shadow-md hover:border-slate-300' : ''
      } ${className}`}
    >
      {(title || subtitle || headerAction) && (
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between gap-4">
          <div>
            {title && (
              <h3 className="text-base font-semibold text-slate-900 tracking-tight">
                {title}
              </h3>
            )}
            {subtitle && (
              <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>
            )}
          </div>
          {headerAction && <div>{headerAction}</div>}
        </div>
      )}
      <div className={noPadding ? '' : 'p-5'}>{children}</div>
      {footer && (
        <div className="px-5 py-3 bg-slate-50/70 border-t border-slate-100 rounded-b-xl">
          {footer}
        </div>
      )}
    </div>
  );
};

export default Card;

