import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import Button from './Button';

export const ErrorState = ({
  title = 'Something went wrong',
  message = 'An error occurred while fetching data. Please try again.',
  onRetry,
  className = '',
}) => {
  return (
    <div
      className={`text-center py-10 px-4 rounded-xl border border-rose-200 bg-rose-50/40 ${className}`}
    >
      <div className="w-12 h-12 rounded-full bg-rose-100 flex items-center justify-center mx-auto text-rose-600 mb-3">
        <AlertCircle className="w-6 h-6" />
      </div>
      <h3 className="text-sm font-semibold text-rose-900">{title}</h3>
      <p className="mt-1 text-xs text-rose-600 max-w-md mx-auto">{message}</p>
      {onRetry && (
        <div className="mt-4">
          <Button
            size="sm"
            variant="outline"
            icon={RefreshCw}
            onClick={onRetry}
            className="border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Retry
          </Button>
        </div>
      )}
    </div>
  );
};

export default ErrorState;

