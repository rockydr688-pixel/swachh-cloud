package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.util.ImageBlurDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BlurDetectorTest {

    private FileAnalysisRepository fileAnalysisRepository;
    private FileItemRepository fileItemRepository;
    private ObjectMapper objectMapper;
    private BlurDetector blurDetector;

    @BeforeEach
    void setUp() {
        fileAnalysisRepository = Mockito.mock(FileAnalysisRepository.class);
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        objectMapper = new ObjectMapper();
        blurDetector = new BlurDetector(fileAnalysisRepository, fileItemRepository, objectMapper);
    }

    private Path createSharpImageFile(Path dir) throws IOException {
        int width = 400;
        int height = 300;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        // Draw sharp high-contrast grid lines
        g.setColor(Color.BLACK);
        for (int x = 0; x < width; x += 15) {
            g.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += 15) {
            g.drawLine(0, y, width, y);
        }
        g.dispose();

        Path path = dir.resolve("sharp-test.png");
        ImageIO.write(img, "png", path.toFile());
        return path;
    }

    private Path createSmoothBlurryImageFile(Path dir) throws IOException {
        int width = 400;
        int height = 300;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // Solid uniform color -> Zero Laplacian variance
        g.setColor(new Color(140, 140, 140));
        g.fillRect(0, 0, width, height);
        g.dispose();

        Path path = dir.resolve("blurry-test.png");
        ImageIO.write(img, "png", path.toFile());
        return path;
    }

    @Test
    void testLocalLaplacianSharpAnalysis(@TempDir Path tempDir) throws IOException {
        Path sharpPath = createSharpImageFile(tempDir);
        ImageBlurDetector.ImageAnalysisResult result = ImageBlurDetector.analyzeImage(sharpPath);

        assertNotNull(result);
        assertTrue(result.variance() >= 250.0, "Sharp grid should yield high Laplacian variance >= 250");
        assertEquals("clear", result.classification());
        assertFalse(result.isBlurry());
        assertTrue(result.blurScore() >= 0.70);
    }

    @Test
    void testLocalLaplacianBlurryAnalysis(@TempDir Path tempDir) throws IOException {
        Path blurryPath = createSmoothBlurryImageFile(tempDir);
        ImageBlurDetector.ImageAnalysisResult result = ImageBlurDetector.analyzeImage(blurryPath);

        assertNotNull(result);
        assertTrue(result.variance() < 100.0, "Smooth uniform image should yield low Laplacian variance < 100");
        assertEquals("heavily_blurry", result.classification());
        assertTrue(result.isBlurry());
        assertTrue(result.blurScore() <= 0.40);
    }

    @Test
    void testAnalyzeImagesEndToEnd_WithResilientFallback(@TempDir Path tempDir) throws IOException {
        Path sharpPath = createSharpImageFile(tempDir);
        Path blurryPath = createSmoothBlurryImageFile(tempDir);

        String userId = "user-blur-test";

        FileItem img1 = new FileItem();
        img1.setId("img-1");
        img1.setName("sharp.png");
        img1.setFileType("image");
        img1.setStorageLocation(sharpPath.toString());

        FileItem img2 = new FileItem();
        img2.setId("img-2");
        img2.setName("blurry.png");
        img2.setFileType("image");
        img2.setStorageLocation(blurryPath.toString());

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(List.of(img1, img2));
        when(fileItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        BlurDetector.BlurAnalysisReport report = blurDetector.analyzeImages(userId, 0.70);

        assertNotNull(report);
        assertEquals(2, report.imagesAnalyzed());
        assertEquals(1, report.blurryCount());
        assertEquals(1, report.clearCount());

        // Verify categories and reasons
        assertEquals("clean", img1.getCategory());
        assertEquals("blurry", img2.getCategory());
        assertEquals("Review & Delete", img2.getRecommendationStatus());
        assertTrue(img2.getDetectionReason().contains("Defective blur"));
    }
}
