import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { StorageConnections } from '../pages/StorageConnections';
import * as StorageContext from '../context/StorageContext';
import { ToastProvider } from '../context/ToastContext';

const mockUseStorage = vi.spyOn(StorageContext, 'useStorage');

const sampleConnections = [
  {
    id: 'conn-local-1',
    name: 'Local Workstation (F:)',
    type: 'local',
    status: 'connected',
    totalSpaceBytes: 1000000000000,
    usedSpaceBytes: 450000000000,
    fileCount: 15400,
    lastScannedAt: '2026-11-18T10:00:00Z',
  },
  {
    id: 'conn-gdrive-1',
    name: 'Google Drive (user@gmail.com)',
    type: 'google_drive',
    status: 'connected',
    totalSpaceBytes: 15000000000,
    usedSpaceBytes: 11000000000,
    fileCount: 4200,
    lastScannedAt: '2026-11-18T10:30:00Z',
  },
  {
    id: 'conn-onedrive-1',
    name: 'OneDrive (user@outlook.com)',
    type: 'onedrive',
    status: 'connected',
    totalSpaceBytes: 5000000000,
    usedSpaceBytes: 3200000000,
    fileCount: 1800,
    lastScannedAt: '2026-11-18T11:00:00Z',
  },
  {
    id: 'conn-dropbox-1',
    name: 'Dropbox (user@dropbox.com)',
    type: 'dropbox',
    status: 'connected',
    totalSpaceBytes: 2000000000,
    usedSpaceBytes: 800000000,
    fileCount: 650,
    lastScannedAt: '2026-11-18T11:15:00Z',
  },
];

describe('StorageConnections Page Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner when loading storage providers', () => {
    mockUseStorage.mockReturnValue({
      connections: [],
      loading: true,
      runScan: vi.fn(),
      refreshData: vi.fn(),
    });

    render(
      <MemoryRouter>
        <ToastProvider>
          <StorageConnections />
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('Querying storage agents & cloud providers...')).toBeDefined();
  });

  it('renders all 4 storage targets with proper badges and multi-cloud architecture banner', () => {
    mockUseStorage.mockReturnValue({
      connections: sampleConnections,
      loading: false,
      runScan: vi.fn(),
      refreshData: vi.fn(),
    });

    render(
      <MemoryRouter>
        <ToastProvider>
          <StorageConnections />
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('4 Storage Targets')).toBeDefined();
    expect(screen.getByText('Universal Multi-Cloud Provider Abstraction')).toBeDefined();
    expect(screen.getByText('Local Workstation (F:)')).toBeDefined();
    expect(screen.getByText('Google Drive (user@gmail.com)')).toBeDefined();
    expect(screen.getByText('OneDrive (user@outlook.com)')).toBeDefined();
    expect(screen.getByText('Dropbox (user@dropbox.com)')).toBeDefined();
  });

  it('opens connect cloud modal when Connect Cloud Account button is clicked', () => {
    mockUseStorage.mockReturnValue({
      connections: sampleConnections,
      loading: false,
      runScan: vi.fn(),
      refreshData: vi.fn(),
    });

    render(
      <MemoryRouter>
        <ToastProvider>
          <StorageConnections />
        </ToastProvider>
      </MemoryRouter>
    );

    const connectButtons = screen.getAllByRole('button', { name: /Connect Cloud Account/i });
    fireEvent.click(connectButtons[0]);

    // Modal should now be open displaying provider options
    expect(screen.getByText('Connect Storage Provider')).toBeDefined();
  });
});
