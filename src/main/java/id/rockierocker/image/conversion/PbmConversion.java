package id.rockierocker.image.conversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

@Slf4j
@Component
public class PbmConversion{

    public ByteArrayOutputStream process(InputStream inputFile, RuntimeException runtimeException) {
        try {
            return processV2(inputFile);
        } catch (Exception e) {
            log.error("Error during PBM conversion: " + e.getMessage(), e);
            throw runtimeException;
        }
    }
    public ByteArrayOutputStream process(InputStream inputFile) throws Exception {
        log.info("Starting conversion to PBM format...");
        // Read source image
        BufferedImage srcImage = ImageIO.read(inputFile);
        if (srcImage == null) {
            throw new IOException("Failed to read image file: " + inputFile);
        }

        // Convert to black and white (binary)
        log.info("Converting to black and white...");
        BufferedImage bw = new BufferedImage(
                srcImage.getWidth(),
                srcImage.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );

        Graphics2D g = bw.createGraphics();
        g.drawImage(srcImage, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(bw, "pbm", output);
        log.info("Conversion to PBM completed");
        return output;
    }

    public ByteArrayOutputStream processV2(InputStream inputFile) throws IOException {

        BufferedImage src = ImageIO.read(inputFile);
        if (src == null) {
            throw new IOException("Invalid image");
        }

        int w = src.getWidth();
        int h = src.getHeight();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(out);

        pw.println("P1");
        pw.println(w + " " + h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int gray = (rgb >> 16 & 0xff)
                        + (rgb >> 8 & 0xff)
                        + (rgb & 0xff);

                pw.print(gray > 382 ? "0 " : "1 ");
            }
            pw.println();
        }

        pw.flush();
        return out;
    }

}
