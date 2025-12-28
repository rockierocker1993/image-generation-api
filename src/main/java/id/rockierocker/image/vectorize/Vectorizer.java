package id.rockierocker.image.vectorize;

import java.nio.file.Path;
import java.util.List;

public interface Vectorizer {
    void vectorize(
            Path inputImage,
            Path outputSvg,
            List<String> additionalCommand
    ) throws Exception;

    String getName();
}
