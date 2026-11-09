package com.cloudclean.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageBlurDetector {

    /**
     * Calculates the normalized sharpness clarity score (0.0 to 1.0)
     * using Laplacian kernel variance on luminance.
     */
    public static Double calculateClarityScore(Path imagePath) {
        try (InputStream is = Files.newInputStream(imagePath)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) return null;

            int width = image.getWidth();
            int height = image.getHeight();
            if (width < 5 || height < 5) return null;

            // Scale down very large images to max 400x400 for speed
            int sampleWidth = Math.min(width, 400);
            int sampleHeight = Math.min(height, 400);
            int[][] gray = new int[sampleWidth][sampleHeight];

            double scaleX = (double) width / sampleWidth;
            double scaleY = (double) height / sampleHeight;

            for (int x = 0; x < sampleWidth; x++) {
                for (int y = 0; y < sampleHeight; y++) {
                    int rgb = image.getRGB((int)(x * scaleX), (int)(y * scaleY));
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // Standard luminance formula
                    gray[x][y] = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                }
            }

            // Apply standard 3x3 Laplacian discrete kernel:
            // [ 0,  1, 0]
            // [ 1, -4, 1]
            // [ 0,  1, 0]
            double sum = 0;
            double sumSq = 0;
            int count = 0;

            for (int x = 1; x < sampleWidth - 1; x++) {
                for (int y = 1; y < sampleHeight - 1; y++) {
                    int laplacian = gray[x + 1][y] + gray[x - 1][y] + gray[x][y + 1] + gray[x][y - 1] - 4 * gray[x][y];
                    sum += laplacian;
                    sumSq += (laplacian * laplacian);
                    count++;
                }
            }

            if (count == 0) return 1.0;
            double mean = sum / count;
            double variance = (sumSq / count) - (mean * mean);

            // Normalize variance to clarity percentage (0.0 to 1.0)
            // Low variance (< 200) indicates high blurriness; > 1000 indicates crisp edges
            double clarity = Math.min(Math.max(variance / 800.0, 0.05), 1.0);
            return Math.round(clarity * 100.0) / 100.0;
        } catch (Exception e) {
            return null;
        }
    }
}
