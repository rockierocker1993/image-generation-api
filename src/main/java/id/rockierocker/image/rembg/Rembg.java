package id.rockierocker.image.rembg;

import java.io.InputStream;
import java.util.Map;

public interface Rembg {
    byte[] removeBackground(InputStream inputImage) throws Exception;
    String getName();
    void configMap(Map<String, Object> config);
}
