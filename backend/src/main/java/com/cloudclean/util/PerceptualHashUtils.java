package com.cloudclean.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Perceptual image hashing utility (dHash / aHash) for detecting visually similar images.
 * Operates independently of exact SHA-256 cryptographic duplicates.
 */
public class PerceptualHashUtils {

    /**
     * Computes the 64-bit difference hash (dHash) from an image path.
     * Resizes image to 9x8 grayscale, compares adjacent horizontal pixels,
     * and produces a 16-character hexadecimal fingerprint.
     */
    public static String computeDHash(Path imagePath) {
        try (InputStream is = Files.newInputStream(imagePath)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) return null;
            return computeDHash(img);
        } catch (Exception e) {
            return null;
        }
    }

    public static String computeDHash(BufferedImage original) {
        if (original == null) return null;

        // Resize to 9 width x 8 height for difference comparison
        BufferedImage resized = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original.getScaledInstance(9, 8, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        long hash = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int left = resized.getRaster().getSample(x, y, 0);
                int right = resized.getRaster().getSample(x + 1, y, 0);
                hash = (hash << 1) | (left > right ? 1 : 0);
            }
        }
        return String.format("%016x", hash);
    }

    /**
     * Computes 64-bit average hash (aHash) by comparing pixels to the image's mean luminance.
     */
    public static String computeAHash(BufferedImage original) {
        if (original == null) return null;

        BufferedImage resized = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original.getScaledInstance(8, 8, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        int sum = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                sum += resized.getRaster().getSample(x, y, 0);
            }
        }
        int avg = sum / 64;

        long hash = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int val = resized.getRaster().getSample(x, y, 0);
                hash = (hash << 1) | (val > avg ? 1 : 0);
            }
        }
        return String.format("%016x", hash);
    }

    /**
     * Calculates the Hamming distance (number of bit positions where bits differ)
     * between two 16-hex perceptual hashes.
     */
    public static int calculateHammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != 16 || hash2.length() != 16) {
            return 64; // Max distance
        }
        long h1 = Long.parseUnsignedLong(hash1, 16);
        long h2 = Long.parseUnsignedLong(hash2, 16);
        return Long.bitCount(h1 ^ h2);
    }

    /**
     * Determines whether two images are visually similar based on a Hamming distance threshold.
     * Distance <= 5 indicates high visual similarity; distance 0 is perceptual identity.
     */
    public static boolean isVisuallySimilar(String hash1, String hash2, int maxDistance) {
        return calculateHammingDistance(hash1, hash2) <= maxDistance;
    }

    public static boolean isVisuallySimilar(String hash1, String hash2) {
        return isVisuallySimilar(hash1, hash2, 5);
    }
}
