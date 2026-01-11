package id.rockierocker.image.rembg;

import ai.onnxruntime.*;
import id.rockierocker.image.refinment.OpenCVPNPRefinment;
import id.rockierocker.image.rembg.constant.OnnxInputSize;
import id.rockierocker.image.util.ImageUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor
@Slf4j
public class OnnxRembgRestApi implements Rembg {

    private Map<String, Object> config;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getName() {
        return "OnnxRembgRestApi";
    }

    @Override
    public void configMap(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public BufferedImage removeBackground(BufferedImage inputImage) throws Exception {
        if (Objects.isNull(config))
            throw new IllegalAccessException("ONNX Rembg not configured yet");

        return process(inputImage);
    }

    /* Process the input image to remove background using ONNX model */
    private BufferedImage process(BufferedImage inputImage) throws Exception {
        log.info("Starting ONNX background removal via REST API...");
        String modelPath = (String) config.get("onnxModel");
        String url = (String) config.get("removeBgApiUrl");
        url = url+"/"+modelPath;
        log.info("Using Remove Background API URL: {}", url);
        log.info("Model path (for reference): {}", modelPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(ImageUtil.toBytesPng(inputImage)) {
            @Override
            public String getFilename() {
                return "image.png";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        log.info("Sending request to Remove Background API at: {}", url);
        Resource response = restTemplate.postForObject(url, requestEntity, Resource.class);
        log.info("Received response from Remove Background API, file size: {} bytes, filename {}", response.contentLength(), response.getFilename());
        return ImageIO.read(response.getInputStream());
    }
}
