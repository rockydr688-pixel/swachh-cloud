package com.cloudclean.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageBlurDetector {

    public static final double DEFAULT_HEAVY_THRESHOLD = 100.0;
    public static final double DEFAULT_MODERATE_THRESHOLD = 250.0;

    public record ImageAnalysisResult(
            double variance,
            double blurScore,
            String classification, // "clear" | "moderately_blurry" | "heavily_blurry"
            boolean isBlurry,
            int width,
            int height,
            double megapixels,
            boolean isLowResolution,
            double meanIntensity,
            boolean isVeryDark,
            boolean isOverexposed,
            double qualityScore,
            String perceptualHash
    ) {}

    /**
     * Preserved legacy method returning clarity score (0.0 to 1.0).
     */
    public static Double calculateClarityScore(Path imagePath) {
        ImageAnalysisResult result = analyzeImage(imagePath);
        return result != null ? result.blurScore() : null;
    }

    /**
     * Performs comprehensive local image analysis on an image file.
     */
    public static ImageAnalysisResult analyzeImage(Path imagePath) {
        try (InputStream is = Files.newInputStream(imagePath)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) return null;
            return analyzeImage(image, DEFAULT_HEAVY_THRESHOLD, DEFAULT_MODERATE_THRESHOLD);
        } catch (Exception e) {
            return null;
        }
    }

    public static ImageAnalysisResult analyzeImage(BufferedImage image, double heavyThresh, double moderateThresh) {
        if (image == null) return null;

        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 5 || height < 5) return null;

        double megapixels = Math.round(((double) width * height / 1_000_000.0) * 1000.0) / 1000.0;
        boolean isLowRes = width < 640 || height < 480 || megapixels < 0.3;

        // Downsample for fast, stable Laplacian evaluation if needed
        int sampleW = Math.min(width, 400);
        int sampleH = Math.min(height, 400);
        int[][] gray = new int[sampleW][sampleH];

        double scaleX = (double) width / sampleW;
        double scaleY = (double) height / sampleH;

        long intensitySum = 0;
        long darkPixelCount = 0;
        long brightPixelCount = 0;
        int totalSampled = sampleW * sampleH;

        for (int x = 0; x < sampleW; x++) {
            for (int y = 0; y < sampleH; y++) {
                int rgb = image.getRGB((int) (x * scaleX), (int) (y * scaleY));
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                gray[x][y] = lum;
                intensitySum += lum;

                if (lum < 25) darkPixelCount++;
                if (lum > 240) brightPixelCount++;
            }
        }

        double meanIntensity = (double) intensitySum / totalSampled;
        boolean isVeryDark = meanIntensity < 35.0 || ((double) darkPixelCount / totalSampled > 0.70);
        boolean isOverexposed = meanIntensity > 220.0 || ((double) brightPixelCount / totalSampled > 0.40);

        // Apply 3x3 discrete Laplacian operator:
        // [ 0,  1, 0]
        // [ 1, -4, 1]
        // [ 0,  1, 0]
        double sum = 0;
        double sumSq = 0;
        int count = 0;

        for (int x = 1; x < sampleW - 1; x++) {
            for (int y = 1; y < sampleH - 1; y++) {
                int lap = gray[x + 1][y] + gray[x - 1][y] + gray[x][y + 1] + gray[x][y - 1] - 4 * gray[x][y];
                sum += lap;
                sumSq += (lap * lap);
                count++;
            }
        }

        double variance = 0.0;
        if (count > 0) {
            double meanLap = sum / count;
            variance = (sumSq / count) - (meanLap * meanLap);
        }
        variance = Math.round(variance * 100.0) / 100.0;

        // Classify blur based on calibrated thresholds
        String classification;
        boolean isBlurry;
        double blurScore;

        if (variance < heavyThresh) {
            classification = "heavily_blurry";
            isBlurry = true;
            blurScore = Math.max(0.0, Math.min(0.4, (variance / Math.max(heavyThresh, 1.0)) * 0.4));
        } else if (variance < moderateThresh) {
            classification = "moderately_blurry";
            isBlurry = true;
            blurScore = 0.4 + ((variance - heavyThresh) / Math.max(moderateThresh - heavyThresh, 1.0)) * 0.35;
        } else {
            classification = "clear";
            isBlurry = false;
            blurScore = Math.min(1.0, 0.75 + Math.min(0.25, (variance - moderateThresh) / 1000.0 * 0.25));
        }
        blurScore = Math.round(blurScore * 100.0) / 100.0;

        // Overall quality score
        double qualityScore = blurScore * 60.0;
        if (!isLowRes) qualityScore += 20.0; else qualityScore += 5.0;
        if (!isVeryDark && !isOverexposed) qualityScore += 20.0; else qualityScore += 5.0;
        qualityScore = Math.round(Math.max(5.0, Math.min(100.0, qualityScore)) * 10.0) / 10.0;

        // Perceptual dHash
        String pHash = PerceptualHashUtils.computeDHash(image);

        return new ImageAnalysisResult(
                variance,
                blurScore,
                classification,
                isBlurry,
                width,
                height,
                megapixels,
                isLowRes,
                Math.round(meanIntensity * 100.0) / 100.0,
                isVeryDark,
                isOverexposed,
                qualityScore,
                pHash
        );
    }
}
