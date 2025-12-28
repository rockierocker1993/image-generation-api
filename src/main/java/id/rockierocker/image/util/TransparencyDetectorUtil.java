package id.rockierocker.image.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class TransparencyDetectorUtil {
    public static boolean hasTransparency(InputStream imageStream, RuntimeException runtimeException) {
        try {

            BufferedImage img = ImageIO.read(imageStream);
            if (img == null) {
                throw new IllegalArgumentException("Invalid image");
            }

            if (!img.getColorModel().hasAlpha()) {
                return false;
            }

            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = img.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xff;

                    if (alpha < 255) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw runtimeException;
        }
    }
    /*
        | Rasio        | Arti                     |
        | ------------ | ------------------------ |
        | `> 0.4`      | background sudah dihapus |
        | `0.05 – 0.4` | icon / cutout            |
        | `< 0.05`     | masih background         |
    */
    public static double transparencyRatio(InputStream imageStream, RuntimeException runtimeException) {
        try {
            BufferedImage img = ImageIO.read(imageStream);
            if (!img.getColorModel().hasAlpha()) return 0.0;

            long transparent = 0;
            long total = (long) img.getWidth() * img.getHeight();

            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                    if (alpha < 250) transparent++;
                }
            }
            return (double) transparent / total;
        } catch (Exception e) {
            throw runtimeException;
        }
    }
}
