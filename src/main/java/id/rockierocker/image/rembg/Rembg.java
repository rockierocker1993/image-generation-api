package id.rockierocker.image.rembg;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;

public interface Rembg {
    BufferedImage removeBackground(BufferedImage inputImage) throws Exception;
    String getName();
    void configMap(Map<String, Object> config);
}
