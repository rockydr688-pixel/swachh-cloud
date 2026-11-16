package com.cloudclean.storage.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter, pagination and traversal options for listing files from a storage provider.
 */
public class ListFilesOptions {

    private int pageSize = 100;
    private String pageToken;
    private Instant modifiedSince; // For incremental scan jobs
    private boolean recursive = true;
    private boolean includeTrashed = false;
    private String mimeTypeFilter;
    private List<String> targetPaths = new ArrayList<>(); // Specific monitored paths (for local storage)
    private Long minSizeBytes;

    public ListFilesOptions() {}

    public static ListFilesOptions defaults() {
        return new ListFilesOptions();
    }

    public static ListFilesOptions forPaths(List<String> paths) {
        ListFilesOptions options = new ListFilesOptions();
        options.setTargetPaths(paths);
        return options;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getPageToken() {
        return pageToken;
    }

    public void setPageToken(String pageToken) {
        this.pageToken = pageToken;
    }

    public Instant getModifiedSince() {
        return modifiedSince;
    }

    public void setModifiedSince(Instant modifiedSince) {
        this.modifiedSince = modifiedSince;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean isIncludeTrashed() {
        return includeTrashed;
    }

    public void setIncludeTrashed(boolean includeTrashed) {
        this.includeTrashed = includeTrashed;
    }

    public String getMimeTypeFilter() {
        return mimeTypeFilter;
    }

    public void setMimeTypeFilter(String mimeTypeFilter) {
        this.mimeTypeFilter = mimeTypeFilter;
    }

    public List<String> getTargetPaths() {
        return targetPaths;
    }

    public void setTargetPaths(List<String> targetPaths) {
        this.targetPaths = targetPaths != null ? targetPaths : new ArrayList<>();
    }

    public Long getMinSizeBytes() {
        return minSizeBytes;
    }

    public void setMinSizeBytes(Long minSizeBytes) {
        this.minSizeBytes = minSizeBytes;
    }
}
