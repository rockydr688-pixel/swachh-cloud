import React from 'react';
import { Loader2 } from 'lucide-react';

export const LoadingSpinner = ({
  size = 'md',
  message = 'Loading...',
  fullPage = false,
}) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-7 h-7',
    lg: 'w-10 h-10',
  };

  const content = (
    <div className="flex flex-col items-center justify-center p-8 text-center animate-fade-in">
      <Loader2
        className={`${sizeClasses[size] || sizeClasses.md} text-indigo-600 animate-spin`}
      />
      {message && (
        <p className="mt-3 text-sm text-slate-500 font-medium">{message}</p>
      )}
    </div>
  );

  if (fullPage) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        {content}
      </div>
    );
  }

  return content;
};

export default LoadingSpinner;

