package id.rockierocker.image.rembg;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ByHexCodeRembg implements Rembg {

    private Map<String, Object> configMap;
    private final String hexColorKey = "hexColorToRemove";

    @Override
    public BufferedImage removeBackground(BufferedImage inputImage) throws Exception {
        log.info("ByHexColorRembg removeBackground called");
        if (Objects.isNull(configMap))
            throw new IllegalAccessException("ByHexColorRembg not configured yet");
        String hexColor = (String) configMap.get(hexColorKey);
        if (Objects.isNull(hexColor))
            throw new IllegalArgumentException("Hex color to remove not configured");

        BufferedImage bufferedImage = removeBackgroundByHex(
                inputImage,
                hexColor,
                0.98  // threshold for color similarity
        );
        return bufferedImage;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void configMap(Map<String, Object> configMap) {
        this.configMap = configMap;
    }

    static int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    public static BufferedImage removeBackgroundByHex(
            BufferedImage input,
            String bgHex,
            double threshold
    ) {
        int width = input.getWidth();
        int height = input.getHeight();

        BufferedImage output =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int[] bg = hexToRgb(bgHex);
        float[] hsvBg = rgbToHsv(bg[0], bg[1], bg[2]);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int pixel = input.getRGB(x, y);

                int a = (pixel >> 24) & 0xff;
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                float[] hsv = rgbToHsv(r, g, b);

                double similarity = colorSimilarity(
                        hsv[0], hsv[1], hsv[2],
                        hsvBg[0], hsvBg[1], hsvBg[2]
                );


                if (similarity >= 0.6)
                    log.info("Pixel at ({}, {}) - Similarity: {}", x, y, similarity);

                if (similarity >= threshold) {
                    // make transparent
                    output.setRGB(x, y, 0x00000000);
                    System.out.println("Making pixel transparent at (" + x + ", " + y + ") with similarity: " + similarity);
                } else {
                    output.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
        }
        return output;
    }

    static double colorSimilarity(int r1, int g1, int b1,
                                  int r2, int g2, int b2) {
        double distance = Math.sqrt(
                Math.pow(r1 - r2, 2) +
                        Math.pow(g1 - g2, 2) +
                        Math.pow(b1 - b2, 2)
        );
        double maxDistance = Math.sqrt(255 * 255 * 3);
        return 1.0 - (distance / maxDistance);
    }

    static float colorSimilarity(float r1, float g1, float b1,
                                 float r2, float g2, float b2) {

        float dr = r1 - r2;
        float dg = g1 - g2;
        float db = b1 - b2;

        float distance = (float) Math.sqrt(
                dr * dr +
                        dg * dg +
                        db * db
        );

        float maxDistance = (float) Math.sqrt(255f * 255f * 3f);

        return 1.0f - (distance / maxDistance);
    }


    static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h;
        if (delta == 0) h = 0;
        else if (max == rf) h = 60 * (((gf - bf) / delta) % 6);
        else if (max == gf) h = 60 * (((bf - rf) / delta) + 2);
        else h = 60 * (((rf - gf) / delta) + 4);

        if (h < 0) h += 360;

        float s = (max == 0) ? 0 : delta / max;
        float v = max;

        return new float[]{h, s, v};
    }

}
