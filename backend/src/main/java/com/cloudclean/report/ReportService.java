package com.cloudclean.report;

import com.cloudclean.cleanup.CleanupHistory;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.CleanupHistoryRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.ScanJobRepository;
import com.cloudclean.scanner.ScanJob;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.Instant;
import java.util.*;

@Service
public class ReportService {

    private final FileItemRepository fileItemRepository;
    private final CleanupHistoryRepository cleanupHistoryRepository;
    private final ScanJobRepository scanJobRepository;

    public ReportService(FileItemRepository fileItemRepository,
                         CleanupHistoryRepository cleanupHistoryRepository,
                         ScanJobRepository scanJobRepository) {
        this.fileItemRepository = fileItemRepository;
        this.cleanupHistoryRepository = cleanupHistoryRepository;
        this.scanJobRepository = scanJobRepository;
    }

    public Map<String, Object> getReportsOverview(String userId) {
        List<FileItem> activeFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);
        List<ScanJob> scans = scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<CleanupHistory> cleanups = cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, 100)).getContent();

        long totalUsedBytes = activeFiles.stream().mapToLong(FileItem::getSizeBytes).sum();
        long totalCleanedBytes = cleanups.stream().filter(c -> !c.isRestored()).mapToLong(CleanupHistory::getFreedSpaceBytes).sum();

        Map<String, Object> report = new HashMap<>();
        report.put("totalFiles", activeFiles.size());
        report.put("totalUsedBytes", totalUsedBytes);
        report.put("totalCleanedBytes", totalCleanedBytes);
        report.put("totalScansExecuted", scans.size());
        report.put("totalCleanupBatches", cleanups.size());

        // Category breakdown
        Map<String, Long> categoryMap = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (FileItem f : activeFiles) {
            String cat = f.getCategory() != null ? f.getCategory() : "clean";
            categoryMap.put(cat, categoryMap.getOrDefault(cat, 0L) + f.getSizeBytes());
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
        }
        report.put("categoryBytes", categoryMap);
        report.put("categoryCounts", categoryCounts);

        // Recent scan reports summary
        List<Map<String, Object>> scanSummaries = scans.stream().limit(10).map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("target", s.getTargetStorage());
            map.put("storageType", s.getStorageType());
            map.put("status", s.getStatus());
            map.put("filesScanned", s.getFilesScanned());
            map.put("totalBytesScanned", s.getTotalBytesScanned());
            map.put("potentialSavingsBytes", s.getPotentialSavingsBytes());
            map.put("completedAt", s.getCompletedAt());
            return map;
        }).toList();
        report.put("scanReports", scanSummaries);

        // Recent cleanup reports summary
        List<Map<String, Object>> cleanupSummaries = cleanups.stream().limit(10).map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("action", c.getAction());
            map.put("category", c.getTargetCategory());
            map.put("filesAffected", c.getFilesAffected());
            map.put("freedBytes", c.getFreedSpaceBytes());
            map.put("status", c.getStatus());
            map.put("timestamp", c.getTimestamp());
            map.put("isRestored", c.isRestored());
            return map;
        }).toList();
        report.put("cleanupReports", cleanupSummaries);

        return report;
    }

    public String exportFilesToCsv(String userId) {
        List<FileItem> files = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);
        StringWriter sw = new StringWriter();
        sw.append("ID,Name,Type,Category,SizeBytes,StorageType,Location,LastModified,JunkScore,RecommendationStatus,IsProtected\n");

        for (FileItem f : files) {
            sw.append(escapeCsv(f.getId())).append(",")
              .append(escapeCsv(f.getName())).append(",")
              .append(escapeCsv(f.getFileType())).append(",")
              .append(escapeCsv(f.getCategory())).append(",")
              .append(String.valueOf(f.getSizeBytes())).append(",")
              .append(escapeCsv(f.getStorageType())).append(",")
              .append(escapeCsv(f.getStorageLocation())).append(",")
              .append(String.valueOf(f.getLastModified())).append(",")
              .append(String.valueOf(f.getJunkScore() != null ? f.getJunkScore() : 0)).append(",")
              .append(escapeCsv(f.getRecommendationStatus())).append(",")
              .append(String.valueOf(f.isProtected())).append("\n");
        }
        return sw.toString();
    }

    public String exportCleanupHistoryToCsv(String userId) {
        List<CleanupHistory> cleanups = cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, 500)).getContent();
        StringWriter sw = new StringWriter();
        sw.append("ID,Timestamp,Action,Category,FilesAffected,FreedSpaceBytes,Status,Method,RestorationSupported,IsRestored\n");

        for (CleanupHistory c : cleanups) {
            sw.append(escapeCsv(c.getId())).append(",")
              .append(String.valueOf(c.getTimestamp())).append(",")
              .append(escapeCsv(c.getAction())).append(",")
              .append(escapeCsv(c.getTargetCategory())).append(",")
              .append(String.valueOf(c.getFilesAffected())).append(",")
              .append(String.valueOf(c.getFreedSpaceBytes())).append(",")
              .append(escapeCsv(c.getStatus())).append(",")
              .append(escapeCsv(c.getMethod())).append(",")
              .append(String.valueOf(c.isRestorationSupported())).append(",")
              .append(String.valueOf(c.isRestored())).append("\n");
        }
        return sw.toString();
    }

    public String exportScanHistoryToCsv(String userId) {
        List<ScanJob> scans = scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
        StringWriter sw = new StringWriter();
        sw.append("ID,TargetStorage,StorageType,Status,FilesScanned,TotalBytesScanned,PotentialSavingsBytes,CreatedAt,CompletedAt\n");

        for (ScanJob s : scans) {
            sw.append(escapeCsv(s.getId())).append(",")
              .append(escapeCsv(s.getTargetStorage())).append(",")
              .append(escapeCsv(s.getStorageType())).append(",")
              .append(escapeCsv(s.getStatus())).append(",")
              .append(String.valueOf(s.getFilesScanned())).append(",")
              .append(String.valueOf(s.getTotalBytesScanned())).append(",")
              .append(String.valueOf(s.getPotentialSavingsBytes())).append(",")
              .append(String.valueOf(s.getCreatedAt())).append(",")
              .append(String.valueOf(s.getCompletedAt())).append("\n");
        }
        return sw.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
