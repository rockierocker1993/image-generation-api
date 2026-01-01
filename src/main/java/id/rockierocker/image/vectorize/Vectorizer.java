package id.rockierocker.image.vectorize;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface Vectorizer {
    void vectorize(
            Path inputImage,
            Path outputSvg,
            List<String> additionalCommand
    ) throws Exception;

    String getName();
}
