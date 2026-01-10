package id.rockierocker.image.config;

import id.rockierocker.image.rembg.OnnxRembg;
import id.rockierocker.image.rembg.Rembg;
import id.rockierocker.image.rembg.constant.OnnxInputSize;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RembgConfig {
    @Bean
    public Rembg rembg() {
        OnnxRembg onnxRembg = new OnnxRembg();
        onnxRembg.configMap(Map.of(
                "onnxModelPath", "./data/onnx-model/isnet-anime.onnx",
                "onnxInputSize", OnnxInputSize.INPUT_SIZE_320
        ));
        return onnxRembg;
    }
}
