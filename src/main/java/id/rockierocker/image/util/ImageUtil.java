package id.rockierocker.image.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageUtil {

    /**
     * Resize a BufferedImage to the given width and height.
     *
     * @param img The original image to be resized.
     * @param w   The target width.
     * @param h   The target height.
     * @return A new BufferedImage that is the resized version of the original image.
     */
    public static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Convert a BufferedImage to a byte array.
     *
     * @param bufferedImage The BufferedImage to be converted.
     * @return A byte array representing the image data.
     * @throws IOException If an error occurs during writing.
     */
    public static byte[] toBytes(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // If the image has an alpha channel, write as PNG to preserve transparency.
        String format = bufferedImage.getColorModel().hasAlpha() ? "png" : "jpg";
        ImageIO.write(bufferedImage, format, baos);
        return baos.toByteArray();
    }

    /**
     * Get the hexadecimal RGBA color of a pixel at (x, y) in the image.
     *
     * @param image The BufferedImage to sample.
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @return A string representing the color in hexadecimal RGBA format.
     */
    public static String getHexRGBA(BufferedImage image, int x, int y) {
        int argb = image.getRGB(x, y);

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        return String.format("#%02X%02X%02X%02X", r, g, b, a);
    }

    /**
     * Get the hexadecimal RGB color of a pixel at (x, y) in the image.
     *
     * @param image The BufferedImage to sample.
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @return A string representing the color in hexadecimal RGB format.
     */
    public static String getHexFromPixel(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Convert an integer RGB value to a hexadecimal color string.
     *
     * @param rgb The integer RGB value.
     * @return A string representing the color in hexadecimal RGB format.
     */
    public static String getHexFast(int rgb) {
        char[] hex = new char[7];
        hex[0] = '#';

        int[] v = {
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF
        };

        final char[] table = "0123456789ABCDEF".toCharArray();

        for (int i = 0; i < 3; i++) {
            hex[i * 2 + 1] = table[v[i] >>> 4];
            hex[i * 2 + 2] = table[v[i] & 0x0F];
        }

        return new String(hex);
    }

    /**
     * Scan the corners of an image and count the occurrence of each hexadecimal color.
     *
     * @param img        The BufferedImage to scan.
     * @param sampleSize The size of the square area to sample at each corner.
     * @return A map with hexadecimal color strings as keys and their occurrence counts as values.
     */
    public static Map<String, Integer> scanCornerHex(BufferedImage img, int sampleSize) {
        Map<String, Integer> counter = new HashMap<>();

        int w = img.getWidth();
        int h = img.getHeight();

        int[][] points = {
                {0, 0}, {w - 1, 0}, {0, h - 1}, {w - 1, h - 1}
        };

        for (int[] p : points) {
            for (int dx = 0; dx < sampleSize; dx++) {
                for (int dy = 0; dy < sampleSize; dy++) {
                    int x = Math.min(p[0] + dx, w - 1);
                    int y = Math.min(p[1] + dy, h - 1);

                    String hex = getHexFast(img.getRGB(x, y));
                    counter.merge(hex, 1, Integer::sum);
                }
            }
        }
        return counter;
    }

    /**
     * Determine if a given hexadecimal color represents a white background.
     *
     * @param hex The hexadecimal color string (e.g., "#FFFFFF").
     * @return True if the color is considered white, false otherwise.
     */
    public static boolean isBackgroundWhite(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);

        return r > 240 && g > 240 && b > 240;
    }

    /**
     * Convert a hexadecimal color string to an RGB integer array.
     *
     * @param hex The hexadecimal color string (e.g., "#RRGGBB").
     * @return An array of integers representing the RGB values.
     */
    public static int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new int[]{r, g, b};
    }

    /**
     * Convert a hexadecimal color string to an RGBA integer array.
     *
     * @param hex The hexadecimal color string (e.g., "#RRGGBBAA").
     * @return An array of integers representing the RGBA values.
     */
    public static int[] hexToRgba(String hex) {
        hex = hex.replace("#", "");

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int a = Integer.parseInt(hex.substring(6, 8), 16);

        return new int[]{r, g, b, a};
    }

    /**
     * Convert a hexadecimal color string to an integer array representing RGB or RGBA values.
     *
     * @param hex The hexadecimal color string (e.g., "#RRGGBB" or "#RRGGBBAA").
     * @return An array of integers representing the RGB or RGBA values.
     */
    public static int[] hexToColor(String hex) {
        hex = hex.replace("#", "");

        if (hex.length() == 6) {
            return new int[]{
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16)
            };
        }

        if (hex.length() == 8) {
            return new int[]{
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16),
                    Integer.parseInt(hex.substring(6, 8), 16)
            };
        }

        throw new IllegalArgumentException("Invalid hex color: " + hex);
    }


}
