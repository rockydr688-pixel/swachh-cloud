package com.cloudclean.storage;

import com.cloudclean.storage.agent.LocalAgentScanner;
import com.cloudclean.storage.provider.ListFilesOptions;
import com.cloudclean.storage.provider.ProviderFileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LocalAgentScannerTest {

    private LocalAgentScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new LocalAgentScanner();
    }

    @Test
    void testPermittedPathValidation(@TempDir Path approvedDir, @TempDir Path otherDir) {
        List<String> approved = List.of(approvedDir.toAbsolutePath().toString());

        Path childFile = approvedDir.resolve("sub/file.txt");
        assertTrue(scanner.isPermittedPath(childFile, approved));

        Path outsideFile = otherDir.resolve("secret.txt");
        assertFalse(scanner.isPermittedPath(outsideFile, approved));

        // Test restricted system directories
        assertFalse(scanner.isPermittedPath(Path.of("C:\\Windows\\System32\\cmd.exe"), approved));
        assertFalse(scanner.isPermittedPath(Path.of("/etc/shadow"), approved));
    }

    @Test
    void testScanCancellation(@TempDir Path tempDir) throws IOException {
        // Create 10 files
        for (int i = 0; i < 10; i++) {
            Files.writeString(tempDir.resolve("file-" + i + ".txt"), "content " + i);
        }

        AtomicBoolean cancelSignal = new AtomicBoolean(true); // Pre-cancelled
        List<ProviderFileMetadata> results = scanner.scanDirectories(
                List.of(tempDir.toAbsolutePath().toString()),
                ListFilesOptions.defaults(),
                cancelSignal::get,
                null
        );

        // Should abort early and return empty or minimal list
        assertTrue(results.isEmpty());
    }

    @Test
    void testIncrementalScanning_FiltersByModifiedDate(@TempDir Path tempDir) throws IOException {
        Path oldFile = Files.writeString(tempDir.resolve("old.txt"), "old file");
        Path newFile = Files.writeString(tempDir.resolve("new.txt"), "new file");

        // Set oldFile modified time to 2 days ago
        Instant twoDaysAgo = Instant.now().minusSeconds(3600 * 48);
        Files.setLastModifiedTime(oldFile, java.nio.file.attribute.FileTime.from(twoDaysAgo));

        // Scan with modifiedSince set to 1 hour ago
        ListFilesOptions options = new ListFilesOptions();
        options.setModifiedSince(Instant.now().minusSeconds(3600));

        List<ProviderFileMetadata> results = scanner.scanDirectories(
                List.of(tempDir.toAbsolutePath().toString()),
                options,
                () -> false,
                null
        );

        assertEquals(1, results.size());
        assertEquals("new.txt", results.get(0).getName());
    }

    @Test
    void testInaccessiblePath_ContinuesGracefully() {
        // Non-existent path
        List<String> paths = List.of("Z:\\Invalid\\Directory\\That\\Does\\Not\\Exist");
        List<ProviderFileMetadata> results = scanner.scanDirectories(
                paths,
                ListFilesOptions.defaults(),
                () -> false,
                null
        );

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
