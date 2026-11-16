package com.cloudclean.storage.provider;

import java.io.InputStream;
import java.util.List;

/**
 * Common storage provider abstraction supporting local host filesystems
 * and enterprise cloud repositories (Google Drive).
 */
public interface StorageProvider {

    /**
     * Lists files from the storage provider according to query/filter options.
     *
     * @param connectionId The storage connection id
     * @param options Query and pagination options (pageSize, pageToken, modifiedSince, etc.)
     * @return List of file metadata records
     */
    List<ProviderFileMetadata> listFiles(String connectionId, ListFilesOptions options);

    /**
     * Retrieves detailed metadata for a single file.
     *
     * @param connectionId The storage connection id
     * @param providerFileId Provider-specific file identifier (or canonical absolute path for local)
     * @return File metadata
     */
    ProviderFileMetadata getFileMetadata(String connectionId, String providerFileId);

    /**
     * Streams file contents for analysis (hashing, blur evaluation, duplicate matching).
     *
     * @param connectionId The storage connection id
     * @param providerFileId Provider-specific file identifier
     * @return Open InputStream for reading file bytes
     */
    InputStream downloadFile(String connectionId, String providerFileId);

    /**
     * Retrieves the raw content bytes of a file up to maxBytes limit.
     *
     * @param connectionId The storage connection id
     * @param providerFileId Provider-specific file identifier
     * @param maxBytes Maximum bytes to read into memory safely
     * @return Byte array of file content
     */
    byte[] getFileContent(String connectionId, String providerFileId, int maxBytes);

    /**
     * Moves a file to the provider's trash / recycle bin.
     *
     * @param connectionId The storage connection id
     * @param providerFileId Provider-specific file identifier
     * @return True if operation was successful
     */
    boolean moveToTrash(String connectionId, String providerFileId);

    /**
     * Restores a previously trashed file.
     *
     * @param connectionId The storage connection id
     * @param providerFileId Provider-specific file identifier
     * @return True if restored successfully
     */
    boolean restoreFile(String connectionId, String providerFileId);

    /**
     * Retrieves total and used storage quota.
     *
     * @param connectionId The storage connection id
     * @return StorageUsage information
     */
    StorageUsage getStorageUsage(String connectionId);

    /**
     * Returns the type identifier of this provider ("local" or "google_drive").
     */
    String getProviderType();

    /**
     * Whether incremental scanning (filtering by modified-since) is supported natively.
     */
    default boolean isIncrementalSupported() {
        return true;
    }
}
