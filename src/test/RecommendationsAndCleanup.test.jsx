import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { CleanupRecommendations } from '../pages/CleanupRecommendations';
import { fileService } from '../services/fileService';

const mockRecommendations = [
  {
    id: 'rec-1',
    fileId: 'file-101',
    fileName: 'redundant_backup.iso',
    description: 'Exact duplicate copy (SHA-256 match)',
    fileType: 'archive',
    fileSize: 4500000000,
    storageSource: 'Local Computer',
    junkScore: 92,
    detectionReasons: ['Exact duplicate copy (SHA-256 match)'],
    recommendedAction: 'Move to safe trash',
    riskLevel: 'low',
    analysisDate: '2026-11-18T10:00:00Z',
    status: 'ACTIVE',
    isProtected: false,
    isExcluded: false,
  },
  {
    id: 'rec-2',
    fileId: 'file-102',
    fileName: 'blurry_party_shot.jpg',
    description: 'Heavily blurry image (Laplacian variance: 42.1)',
    fileType: 'image',
    fileSize: 8500000,
    storageSource: 'Google Drive',
    junkScore: 78,
    detectionReasons: ['Heavily blurry image (Laplacian variance: 42.1)'],
    recommendedAction: 'Move to cloud trash',
    riskLevel: 'low',
    analysisDate: '2026-11-18T10:05:00Z',
    status: 'ACTIVE',
    isProtected: false,
    isExcluded: false,
  },
];

describe('Cleanup Recommendations & Confirmation Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(fileService, 'getRecommendations').mockResolvedValue({
      success: true,
      data: mockRecommendations,
    });
    vi.spyOn(fileService, 'calculateCleanupImpact').mockResolvedValue({
      totalFilesSelected: 2,
      totalBytesRecoverable: 4508500000,
      formattedSize: '4.2 GB',
      protectedCount: 0,
      highRiskCount: 0,
      driveBreakdown: { localBytes: 4500000000, googleDriveBytes: 8500000 },
    });
    vi.spyOn(fileService, 'executeCleanup').mockResolvedValue({
      success: true,
      message: 'Cleanup successfully moved 2 files to trash.',
    });
  });

  it('renders list of recommendations with risk badges and scores', async () => {
    render(
      <MemoryRouter>
        <CleanupRecommendations />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('redundant_backup.iso')).toBeDefined();
      expect(screen.getByText('blurry_party_shot.jpg')).toBeDefined();
    });

    expect(screen.getByText(/Exact duplicate copy/i)).toBeDefined();
    expect(screen.getByText(/Heavily blurry image/i)).toBeDefined();
    expect(screen.getByText('Score: 92/100')).toBeDefined();
    expect(screen.getByText('Score: 78/100')).toBeDefined();
  });

  it('enforces explicit user confirmation before enabling Execute Cleanup button', async () => {
    render(
      <MemoryRouter>
        <CleanupRecommendations />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('redundant_backup.iso')).toBeDefined();
    });

    // Select all recommendations via the header checkbox
    const selectAllCheckbox = screen.getByLabelText('Select All');
    fireEvent.click(selectAllCheckbox);

    // Click "Clean Selected (2)" button
    const cleanSelectedBtn = await screen.findByRole('button', { name: /Clean Selected/i });
    expect(cleanSelectedBtn).toBeDefined();
    fireEvent.click(cleanSelectedBtn);

    // Modal opens
    await waitFor(() => {
      expect(screen.getByText('Confirm Cleanup Operation')).toBeDefined();
    });

    // The execute button should initially be disabled until explicit confirmation checkbox is checked
    const executeBtn = screen.getByRole('button', { name: /Confirm & Move to Trash/i });
    expect(executeBtn.disabled).toBe(true);

    // Toggle explicit confirmation checkbox
    const confirmCheckbox = screen.getByRole('checkbox', {
      name: /I explicitly confirm that I want to move these selected items to trash/i,
    });
    fireEvent.click(confirmCheckbox);

    // Now execute button must be enabled
    expect(executeBtn.disabled).toBe(false);

    // Execute cleanup
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(fileService.executeCleanup).toHaveBeenCalledWith(
        expect.objectContaining({
          confirmed: true,
        })
      );
    });
  });
});
