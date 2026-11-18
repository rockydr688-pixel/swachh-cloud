import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Dashboard } from '../pages/Dashboard';
import * as StorageContext from '../context/StorageContext';

const mockUseStorage = vi.spyOn(StorageContext, 'useStorage');

const sampleStats = {
  totalCapacityBytes: 1000000000000, // 1 TB
  totalUsedBytes: 450000000000,      // 450 GB
  recoverableBytes: 120000000000,    // 120 GB
  totalFiles: 28450,
  duplicateCount: 1420,
  duplicateBytes: 45000000000,
  blurryImagesCount: 310,
  blurryImagesBytes: 15000000000,
  largeFilesCount: 85,
  largeFilesBytes: 50000000000,
  inactiveFilesCount: 920,
  inactiveFilesBytes: 10000000000,
  healthScore: 88,
  actualRecoveredBytes: 25000000000,
};

describe('Dashboard Component Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner when data is loading', () => {
    mockUseStorage.mockReturnValue({
      stats: null,
      recommendations: [],
      recentScans: [],
      loading: true,
      runScan: vi.fn(),
      isScanning: false,
    });

    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );

    expect(screen.getByText('Aggregating storage telemetry...')).toBeDefined();
  });

  it('renders all telemetry cards and metrics accurately when data is loaded', () => {
    const mockRunScan = vi.fn();
    mockUseStorage.mockReturnValue({
      stats: sampleStats,
      recommendations: [],
      recentScans: [],
      loading: false,
      runScan: mockRunScan,
      isScanning: false,
    });

    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );

    // Header checks
    expect(screen.getByText('Storage Intelligence Dashboard')).toBeDefined();
    expect(screen.getByText('AI Engine v2.4')).toBeDefined();

    // Metric cards
    expect(screen.getByText('Total Storage Pool Capacity')).toBeDefined();
    expect(screen.getByText('Recoverable Savings')).toBeDefined();
    expect(screen.getByText('Exact Duplicates:')).toBeDefined();
    expect(screen.getByText('Blurry Photos:')).toBeDefined();
    expect(screen.getByText('Stale Large Files:')).toBeDefined();

    // Telemetry Action Buttons
    expect(screen.getByRole('button', { name: /Refresh Telemetry/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Review Recommendations/i })).toBeDefined();
  });

  it('renders empty state when no files have been scanned yet (0 files in MongoDB)', () => {
    mockUseStorage.mockReturnValue({
      stats: {
        totalCapacityBytes: 1000000000000,
        totalUsedBytes: 0,
        recoverableBytes: 0,
        totalFiles: 0,
      },
      recommendations: [],
      recentScans: [],
      loading: false,
      runScan: vi.fn(),
      isScanning: false,
    });

    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );

    expect(screen.getByText('No files scanned yet. Connect your storage to get started.')).toBeDefined();
    expect(screen.getByRole('button', { name: /Connect Storage/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Start First Scan/i })).toBeDefined();
  });
});
