import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { storageService } from '../services/storageService';
import { fileService } from '../services/fileService';
import { scanService } from '../services/scanService';

const StorageContext = createContext(null);

export const StorageProvider = ({ children }) => {
  const [stats, setStats] = useState(null);
  const [connections, setConnections] = useState([]);
  const [recentScans, setRecentScans] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanProgress, setScanProgress] = useState(0);
  const [activeScan, setActiveScan] = useState(null);
  const [scanError, setScanError] = useState(null);

  const pollIntervalRef = useRef(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsRes, connRes, scansRes, recsRes] = await Promise.all([
        storageService.getStorageStats(),
        storageService.getConnections(),
        scanService.getRecentScans(),
        fileService.getRecommendations(),
      ]);
      setStats(statsRes.data);
      setConnections(connRes.data);
      setRecentScans(scansRes.data);
      setRecommendations(recsRes.data);
    } catch (err) {
      setError(err.message || 'Failed to load storage data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    return () => {
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
    };
  }, []);

  const runScan = async (optionsOrTarget = 'Local Computer') => {
    if (isScanning) return;

    setScanError(null);
    const options = typeof optionsOrTarget === 'string'
      ? { target: optionsOrTarget }
      : optionsOrTarget;

    const targetName = options.target || 'Local Computer';
    const isGoogle = targetName.toLowerCase().includes('google');
    const storageType = options.storageType || (isGoogle ? 'google_drive' : 'local');

    // Find connection id if available
    const matchedConn = connections.find((c) =>
      c.type === storageType || c.name.toLowerCase() === targetName.toLowerCase()
    );

    setIsScanning(true);
    setScanProgress(5);
    setActiveScan({
      target: targetName,
      storageType,
      progressPercent: 5,
      totalFilesScanned: 0,
      failedFilesCount: 0,
      totalBytesScanned: 0,
      activeDirectory: 'Initializing scanner agent...',
      status: 'in_progress',
    });

    try {
      const startResult = await scanService.startScan({
        target: targetName,
        storageType,
        storageConnectionId: matchedConn?.id,
        incremental: options.incremental ?? false,
        checkDuplicates: options.checkDuplicates ?? true,
        checkBlurry: options.checkBlurry ?? true,
        checkLargeFiles: options.checkLargeFiles ?? true,
        checkInactive: options.checkInactive ?? true,
        largeFileSizeThresholdMB: options.largeFileSizeThresholdMB,
        blurThreshold: options.blurThreshold,
      });

      const scanId = startResult.scanId;

      // Poll real progress from backend
      pollIntervalRef.current = setInterval(async () => {
        try {
          const liveStatus = await scanService.getScanProgress(scanId);
          if (liveStatus) {
            setScanProgress(liveStatus.progressPercent || 0);
            setActiveScan({
              id: scanId,
              target: liveStatus.target || targetName,
              storageType: liveStatus.storageType || storageType,
              progressPercent: liveStatus.progressPercent || 0,
              totalFilesScanned: liveStatus.totalFilesScanned || 0,
              failedFilesCount: liveStatus.failedFilesCount || 0,
              totalBytesScanned: liveStatus.totalBytesScanned || 0,
              activeDirectory: liveStatus.activeDirectory || 'Scanning file trees...',
              potentialSavingsBytes: liveStatus.potentialSavingsBytes || 0,
              flaggedFilesCount: liveStatus.flaggedFilesCount || 0,
              duration: liveStatus.duration,
              status: liveStatus.status,
            });

            if (liveStatus.status === 'completed' || liveStatus.status === 'cancelled' || liveStatus.status === 'failed') {
              clearInterval(pollIntervalRef.current);
              setIsScanning(false);
              // Refresh recent scans and dashboard metrics
              const scansRes = await scanService.getRecentScans();
              setRecentScans(scansRes.data);
              storageService.getStorageStats().then((res) => setStats(res.data));
              storageService.getConnections().then((res) => setConnections(res.data));
            }
          }
        } catch (pollErr) {
          console.warn('Poll error for scan:', pollErr);
        }
      }, 700);

    } catch (err) {
      setIsScanning(false);
      setScanProgress(0);
      setActiveScan(null);
      setScanError(err.message || 'Could not initiate scan job');
      throw err;
    }
  };

  const cancelActiveScan = async () => {
    if (!activeScan?.id) {
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
      setIsScanning(false);
      setActiveScan(null);
      return;
    }

    try {
      await scanService.cancelScan(activeScan.id);
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
      setIsScanning(false);
      setActiveScan((prev) => prev ? { ...prev, status: 'cancelled' } : null);
      const scansRes = await scanService.getRecentScans();
      setRecentScans(scansRes.data);
    } catch (err) {
      console.error('Failed to cancel scan:', err);
    }
  };

  const connectStorage = async (type, payload) => {
    const res = await storageService.connectStorage(type, payload);
    const connRes = await storageService.getConnections();
    setConnections(connRes.data);
    return res;
  };

  const disconnectStorage = async (id) => {
    const res = await storageService.disconnectStorage(id);
    const connRes = await storageService.getConnections();
    setConnections(connRes.data);
    return res;
  };

  const executeRecommendation = async (recId) => {
    await fileService.executeCleanup(recId);
    setRecommendations((prev) => prev.filter((r) => r.id !== recId));
  };

  return (
    <StorageContext.Provider
      value={{
        stats,
        connections,
        recentScans,
        recommendations,
        loading,
        error,
        isScanning,
        scanProgress,
        activeScan,
        scanError,
        refreshData: fetchData,
        runScan,
        cancelActiveScan,
        connectStorage,
        disconnectStorage,
        executeRecommendation,
      }}
    >
      {children}
    </StorageContext.Provider>
  );
};

export const useStorage = () => {
  const context = useContext(StorageContext);
  if (!context) {
    throw new Error('useStorage must be used within a StorageProvider');
  }
  return context;
};

export default StorageContext;
