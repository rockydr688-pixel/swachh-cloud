import React, { createContext, useContext, useState, useEffect } from 'react';
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
  }, []);

  // Simulated live scanner
  const runScan = (targetName = 'Local Computer') => {
    if (isScanning) return;
    setIsScanning(true);
    setScanProgress(0);

    const interval = setInterval(() => {
      setScanProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setIsScanning(false);
          // Add a new scan record
          const newScan = {
            id: `scan-${Date.now().toString().slice(-4)}`,
            target: targetName,
            storageType: targetName.toLowerCase().includes('google') ? 'google_drive' : 'local',
            startTime: new Date(Date.now() - 45000).toISOString(),
            completedTime: new Date().toISOString(),
            duration: '45s',
            filesScanned: 18450,
            status: 'completed',
            potentialSavingsBytes: 42000000000,
            flaggedFilesCount: 412,
          };
          setRecentScans((curr) => [newScan, ...curr]);
          return 100;
        }
        return prev + 10;
      });
    }, 400);
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
        refreshData: fetchData,
        runScan,
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

