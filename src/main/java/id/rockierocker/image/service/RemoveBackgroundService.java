package id.rockierocker.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class RemoveBackgroundService {

    private final RestTemplate restTemplate;
    @Value("${remove-bg.url}")
    private String url;

    public byte[] removeBackground(File inputFile) {
        try {
            log.info("Removing background for file: {}", inputFile.getName());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(inputFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Sending request to Remove Background API at: {}", url);
            Resource response = restTemplate.postForObject(url, requestEntity, Resource.class);
            log.info("Received response from Remove Background API, file size: {} bytes, filename {}", response.contentLength(), response.getFilename());
            return response.getContentAsByteArray();

        }catch (Exception e){
            log.error("Error while removing background: ", e);
            return null;
        }
    }
}
