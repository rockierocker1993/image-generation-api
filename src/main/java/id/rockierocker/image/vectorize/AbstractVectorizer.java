package id.rockierocker.image.vectorize;

import id.rockierocker.image.util.ImageUtil;
import id.rockierocker.image.vectorize.model.Color;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
abstract class AbstractVectorizer {

    // =========================
    // CONFIG
    // =========================
    static final int K_COLORS = 5;        // recommended 4–6
    static final float CONTRAST = 1.2f;   // 1.1 – 1.3
    static final int ITERATIONS = 10;

    static final float[][] SHARPEN_KERNEL = {
            { 0, -1,  0 },
            { -1,  5, -1 },
            { 0, -1,  0 }
    };

    protected void exec(ProcessBuilder pb, String name) throws Exception {
        log.info("Executing {} command: {}", name, String.join(" ", pb.command()));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        log.info("{} output: \n{}", name, output.toString());
        if (p.waitFor() != 0) {
            throw new RuntimeException(name + " failed");
        }
    }

    protected void preProcess(Path inputPath) throws Exception {
        BufferedImage bufferedImage = ImageIO.read(inputPath.toFile());
        bufferedImage = kMeansQuantization(bufferedImage, K_COLORS);
        bufferedImage = adjustContrast(bufferedImage, CONTRAST);
        bufferedImage = sharpen(bufferedImage);
        ImageIO.write(bufferedImage, "PNG", inputPath.toFile());
    }

    // =========================
    // K-MEANS QUANTIZATION
    // =========================
    static BufferedImage kMeansQuantization(BufferedImage img, int k) {
        int w = img.getWidth();
        int h = img.getHeight();

        List<Color> pixels = new ArrayList<>(w * h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                pixels.add(new Color(
                        (rgb >> 16) & 0xff,
                        (rgb >> 8) & 0xff,
                        rgb & 0xff
                ));
            }
        }

        Random rand = new Random();
        List<Color> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            centroids.add(pixels.get(rand.nextInt(pixels.size())));
        }

        for (int iter = 0; iter < ITERATIONS; iter++) {
            List<List<Color>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) clusters.add(new ArrayList<>());

            for (Color p : pixels) {
                int idx = nearestCentroid(p, centroids);
                clusters.get(idx).add(p);
            }

            for (int i = 0; i < k; i++) {
                if (clusters.get(i).isEmpty()) continue;
                float r = 0, g = 0, b = 0;
                for (Color c : clusters.get(i)) {
                    r += c.r;
                    g += c.g;
                    b += c.b;
                }
                centroids.set(i, new Color(
                        r / clusters.get(i).size(),
                        g / clusters.get(i).size(),
                        b / clusters.get(i).size()
                ));
            }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color p = pixels.get(idx++);
                Color c = centroids.get(nearestCentroid(p, centroids));
                int rgb =
                        ((int) c.r << 16) |
                                ((int) c.g << 8) |
                                (int) c.b;
                out.setRGB(x, y, rgb);
            }
        }
        return out;
    }

    static int nearestCentroid(Color p, List<Color> centroids) {
        float min = Float.MAX_VALUE;
        int idx = 0;
        for (int i = 0; i < centroids.size(); i++) {
            float d = distance(p, centroids.get(i));
            if (d < min) {
                min = d;
                idx = i;
            }
        }
        return idx;
    }

    static float distance(Color a, Color b) {
        float dr = a.r - b.r;
        float dg = a.g - b.g;
        float db = a.b - b.b;
        return dr * dr + dg * dg + db * db;
    }

    // =========================
    // CONTRAST
    // =========================
    static BufferedImage adjustContrast(BufferedImage img, float factor) {
        int w = img.getWidth();
        int h = img.getHeight();

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);

                int r = clamp((int) (( ((rgb >> 16) & 0xff) - 128) * factor + 128));
                int g = clamp((int) (( ((rgb >> 8) & 0xff) - 128) * factor + 128));
                int b = clamp((int) (( (rgb & 0xff) - 128) * factor + 128));

                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    // =========================
    // SHARPEN
    // =========================
    static BufferedImage sharpen(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int r = applyKernel(img, x, y, 16);
                int g = applyKernel(img, x, y, 8);
                int b = applyKernel(img, x, y, 0);
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    static int applyKernel(BufferedImage img, int x, int y, int shift) {
        float sum = 0;
        for (int ky = -1; ky <= 1; ky++) {
            for (int kx = -1; kx <= 1; kx++) {
                int rgb = img.getRGB(x + kx, y + ky);
                int v = (rgb >> shift) & 0xff;
                sum += v * SHARPEN_KERNEL[ky + 1][kx + 1];
            }
        }
        return clamp((int) sum);
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
