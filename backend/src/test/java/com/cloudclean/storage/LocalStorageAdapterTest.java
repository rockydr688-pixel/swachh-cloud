package com.cloudclean.storage;

import com.cloudclean.exception.ForbiddenException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.agent.LocalAgentScanner;
import com.cloudclean.storage.provider.ListFilesOptions;
import com.cloudclean.storage.provider.LocalStorageAdapter;
import com.cloudclean.storage.provider.ProviderFileMetadata;
import com.cloudclean.util.FileHashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class LocalStorageAdapterTest {

    private StorageConnectionRepository connectionRepository;
    private LocalAgentScanner localAgentScanner;
    private LocalStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        connectionRepository = Mockito.mock(StorageConnectionRepository.class);
        localAgentScanner = new LocalAgentScanner();
        adapter = new LocalStorageAdapter(connectionRepository, localAgentScanner);
    }

    @Test
    void testListFiles_NestedFolderTraversalAndMetadata(@TempDir Path tempDir) throws IOException {
        // Setup nested directory structure
        Path subDir1 = Files.createDirectory(tempDir.resolve("folderA"));
        Path subDir2 = Files.createDirectory(subDir1.resolve("nestedB"));

        Path file1 = Files.writeString(tempDir.resolve("root-file.txt"), "Hello World");
        Path file2 = Files.writeString(subDir1.resolve("image-sample.png"), "sample-png-bytes");
        Path file3 = Files.writeString(subDir2.resolve("archive.zip"), "zip-content-data");

        String connectionId = "conn-local-1";
        StorageConnection conn = new StorageConnection("user-123", "Local Test", "local");
        conn.setId(connectionId);
        conn.setMonitoredPaths(List.of(tempDir.toAbsolutePath().toString()));

        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(conn));

        List<ProviderFileMetadata> files = adapter.listFiles(connectionId, ListFilesOptions.defaults());

        assertNotNull(files);
        assertEquals(3, files.size());

        // Validate metadata properties
        ProviderFileMetadata rootMeta = files.stream()
                .filter(f -> f.getName().equals("root-file.txt"))
                .findFirst().orElseThrow();
        assertEquals("txt", rootMeta.getExtension());
        assertEquals("text/plain", rootMeta.getMimeType());
        assertTrue(rootMeta.getSizeBytes() > 0);
        assertNotNull(rootMeta.getCreatedTime());
        assertNotNull(rootMeta.getModifiedTime());
        assertNotNull(rootMeta.getSha256Hash());
        assertEquals(FileHashUtils.computeSha256(file1), rootMeta.getSha256Hash());
        assertFalse(rootMeta.isTrashed());
    }

    @Test
    void testPathTraversal_RejectsUnauthorizedPath(@TempDir Path tempDir, @TempDir Path unauthorizedDir) {
        String connectionId = "conn-local-2";
        StorageConnection conn = new StorageConnection("user-123", "Local Test", "local");
        conn.setId(connectionId);
        conn.setMonitoredPaths(List.of(tempDir.toAbsolutePath().toString()));

        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(conn));

        // Attempting to read outside approved tempDir should throw ForbiddenException
        Path outsideFile = unauthorizedDir.resolve("secret.txt");
        assertThrows(ForbiddenException.class, () ->
                adapter.getFileMetadata(connectionId, outsideFile.toAbsolutePath().toString())
        );

        assertThrows(ForbiddenException.class, () ->
                adapter.downloadFile(connectionId, outsideFile.toAbsolutePath().toString())
        );
    }

    @Test
    void testMoveToTrashAndRestore(@TempDir Path tempDir) throws IOException {
        Path fileToTrash = Files.writeString(tempDir.resolve("temporary-doc.pdf"), "content to be safely trashed");

        String connectionId = "conn-local-trash";
        StorageConnection conn = new StorageConnection("user-123", "Local Trash Test", "local");
        conn.setId(connectionId);
        conn.setMonitoredPaths(List.of(tempDir.toAbsolutePath().toString()));

        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(conn));

        assertTrue(Files.exists(fileToTrash));

        // Move to trash
        boolean trashed = adapter.moveToTrash(connectionId, fileToTrash.toAbsolutePath().toString());
        assertTrue(trashed);
        assertFalse(Files.exists(fileToTrash), "Original file should no longer be at original path");

        // Verify trash directory exists inside approved folder
        Path trashDir = tempDir.resolve(".cloudclean_trash");
        assertTrue(Files.exists(trashDir));
        Path inTrash = trashDir.resolve("temporary-doc.pdf");
        assertTrue(Files.exists(inTrash));

        // Restore file
        boolean restored = adapter.restoreFile(connectionId, fileToTrash.toAbsolutePath().toString());
        assertTrue(restored);
        assertTrue(Files.exists(fileToTrash), "File should be restored to original path");
        assertFalse(Files.exists(inTrash), "File should be moved out of trash directory");
    }

    @Test
    void testDownloadFileAndContentReading(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("data.csv"), "col1,col2\nval1,val2\n");

        String connectionId = "conn-download";
        StorageConnection conn = new StorageConnection("user-123", "Download Test", "local");
        conn.setId(connectionId);
        conn.setMonitoredPaths(List.of(tempDir.toAbsolutePath().toString()));

        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(conn));

        try (InputStream is = adapter.downloadFile(connectionId, file.toAbsolutePath().toString())) {
            assertNotNull(is);
            byte[] bytes = is.readAllBytes();
            assertTrue(bytes.length > 0);
            assertEquals("col1,col2\nval1,val2\n", new String(bytes));
        }

        byte[] preview = adapter.getFileContent(connectionId, file.toAbsolutePath().toString(), 1024);
        assertNotNull(preview);
        assertEquals("col1,col2\nval1,val2\n", new String(preview));
    }
}
