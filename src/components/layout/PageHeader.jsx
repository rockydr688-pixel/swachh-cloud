import React from 'react';
import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export const PageHeader = ({
  title,
  subtitle,
  breadcrumbs = [],
  actions,
  badge,
}) => {
  return (
    <div className="mb-6 sm:mb-8 pb-5 border-b border-slate-200/80 flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        {breadcrumbs.length > 0 && (
          <nav className="flex items-center gap-1.5 text-xs text-slate-400 mb-2 font-medium">
            <Link to="/dashboard" className="hover:text-slate-600 transition-colors">
              Home
            </Link>
            {breadcrumbs.map((crumb, idx) => (
              <React.Fragment key={crumb.label || idx}>
                <ChevronRight className="w-3.5 h-3.5 text-slate-300" />
                {crumb.path ? (
                  <Link
                    to={crumb.path}
                    className="hover:text-slate-600 transition-colors"
                  >
                    {crumb.label}
                  </Link>
                ) : (
                  <span className="text-slate-600 font-semibold">
                    {crumb.label}
                  </span>
                )}
              </React.Fragment>
            ))}
          </nav>
        )}

        <div className="flex items-center gap-3">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            {title}
          </h1>
          {badge}
        </div>
        {subtitle && (
          <p className="mt-1 text-xs sm:text-sm text-slate-500 max-w-2xl">
            {subtitle}
          </p>
        )}
      </div>

      {actions && (
        <div className="flex items-center gap-2.5 shrink-0 flex-wrap">
          {actions}
        </div>
      )}
    </div>
  );
};

export default PageHeader;

