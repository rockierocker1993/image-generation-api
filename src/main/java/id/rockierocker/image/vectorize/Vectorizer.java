package id.rockierocker.image.vectorize;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface Vectorizer {
    byte[] vectorize(
            Path inputImage,
            List<String> additionalCommand,
            RuntimeException runtimeException
    );

    byte[] vectorize(
            Path inputImage,
            List<String> additionalCommand
    ) throws Exception;

    String getName();
}
