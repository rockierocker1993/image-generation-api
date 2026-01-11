package id.rockierocker.image.vectorize;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
abstract class AbstractVectorizer {

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

    protected Path getOutputPath() throws IOException {
        File tempFile = File.createTempFile("vector", ".png");
        return tempFile.toPath();
    }

    protected BufferedImage readAndDelete(Path file) throws IOException {
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                log.warn("Unsupported image format");
                throw new RuntimeException("Unsupported image format");
            }
            return image;
        } finally {
            Files.deleteIfExists(file);
        }
    }


}
