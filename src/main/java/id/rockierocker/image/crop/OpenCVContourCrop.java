package id.rockierocker.image.crop;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class OpenCVContourCrop implements Crop {

    /**
     * Padding (in pixels) to add around detected regions to avoid cutting edges.
     * Can be adjusted based on image type and quality requirements.
     * Default: 1 pixel
     */
    private int croppingPadding = 8;

    /**
     * Set the padding for cropping
     * @param padding padding in pixels (recommended: 0-10)
     */
    public void setCroppingPadding(int padding) {
        this.croppingPadding = Math.max(0, padding);
        log.info("Cropping padding set to: {} pixels", this.croppingPadding);
    }

    /**
     * Get the current cropping padding
     * @return padding in pixels
     */
    public int getCroppingPadding() {
        return this.croppingPadding;
    }

    @Override
    public List<BufferedImage> crop(BufferedImage inputImage) {
        log.info("Starting contour-based cropping using OpenCV...");
        log.info("Input image size: {}x{}, type: {}", inputImage.getWidth(), inputImage.getHeight(), inputImage.getType());

        List<BufferedImage> results = new ArrayList<>();

        // Detect if image has alpha channel
        boolean hasAlpha = inputImage.getColorModel().hasAlpha();
        log.info("Image has alpha channel: {}", hasAlpha);

        // 1️⃣ Convert BufferedImage → Mat (preserving alpha if present)
        Mat src = bufferedImageToMat(inputImage);

        // 2️⃣ Create mask based on alpha channel or brightness
        Mat mask = new Mat();

        if (src.channels() == 4 && hasAlpha) {
            // For transparent images: use alpha channel directly
            log.info("Using alpha channel for mask detection");
            List<Mat> channels = new ArrayList<>();
            Core.split(src, channels);
            Mat alphaChannel = channels.get(3); // BGRA format, alpha is channel 3

            // Threshold alpha: any pixel with alpha > 10 is considered visible
            Imgproc.threshold(alphaChannel, mask, 10, 255, Imgproc.THRESH_BINARY);

            // Clean up channels
            for (Mat channel : channels) {
                if (channel != alphaChannel) {
                    channel.release();
                }
            }
            alphaChannel.release();
        } else {
            // For opaque images: use brightness/grayscale
            log.info("Using grayscale for mask detection");
            Mat gray = new Mat();
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

            // Use adaptive threshold or Otsu for better edge detection
            Imgproc.threshold(gray, mask, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
            gray.release();
        }

        // 3️⃣ Apply morphology to connect nearby regions (optional, more gentle)
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

        // Dilate slightly to ensure we don't cut anti-aliased edges
        Imgproc.dilate(mask, mask, kernel, new Point(-1, -1), 2);

        // 4️⃣ Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        log.info("Found {} contours", contours.size());

        // 5️⃣ Sort contour: left → right, top → bottom
        contours.sort(Comparator.comparingInt(c -> {
            Rect r = Imgproc.boundingRect(c);
            return r.y * 10000 + r.x;
        }));

        // 6️⃣ Crop each contour with padding
        int validContours = 0;

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);

            // Filter noise based on area
            if (rect.area() < 1500) {
                log.debug("Skipping small contour with area: {}", rect.area());
                continue;
            }

            // Add padding but ensure we stay within image bounds
            int x = Math.max(0, rect.x - croppingPadding);
            int y = Math.max(0, rect.y - croppingPadding);
            int width = Math.min(src.cols() - x, rect.width + 2 * croppingPadding);
            int height = Math.min(src.rows() - y, rect.height + 2 * croppingPadding);

            Rect paddedRect = new Rect(x, y, width, height);

            log.info("Cropping contour {} with area: {}, bounds: {}x{} at ({}, {}), padded to {}x{} at ({}, {}) [padding={}px]",
                    validContours + 1, rect.area(), rect.width, rect.height, rect.x, rect.y,
                    paddedRect.width, paddedRect.height, paddedRect.x, paddedRect.y, croppingPadding);

            Mat cropped = new Mat(src, paddedRect);
            BufferedImage croppedImage = matToBufferedImage(cropped);
            results.add(croppedImage);
            validContours++;
        }

        log.info("Successfully cropped {} valid regions", validContours);

        // Clean up
        src.release();
        mask.release();
        kernel.release();
        hierarchy.release();

        return results;
    }

    private Mat bufferedImageToMat(BufferedImage bi) {
        // Determine if we need to preserve alpha channel
        boolean hasAlpha = bi.getColorModel().hasAlpha();
        BufferedImage convertedImage;
        Mat mat;

        if (hasAlpha) {
            // Convert to 4BYTE_ABGR for OpenCV BGRA format
            log.debug("Converting BufferedImage with alpha (type: {}) to 4BYTE_ABGR", bi.getType());
            convertedImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            convertedImage.getGraphics().drawImage(bi, 0, 0, null);

            mat = new Mat(convertedImage.getHeight(), convertedImage.getWidth(), CvType.CV_8UC4);
            byte[] data = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else {
            // Convert to BGR format for OpenCV
            if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                log.debug("Converting BufferedImage from type {} to TYPE_3BYTE_BGR", bi.getType());
                convertedImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                convertedImage.getGraphics().drawImage(bi, 0, 0, null);
            } else {
                convertedImage = bi;
            }

            mat = new Mat(convertedImage.getHeight(), convertedImage.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        }

        return mat;
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type;

        // Determine output type based on channels
        if (mat.channels() == 4) {
            // BGRA -> convert to INT_ARGB for high quality
            type = BufferedImage.TYPE_INT_ARGB;
            byte[] data = new byte[mat.rows() * mat.cols() * 4];
            mat.get(0, 0, data);

            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
            int[] pixels = new int[mat.cols() * mat.rows()];

            // Convert BGRA to ARGB
            for (int i = 0; i < pixels.length; i++) {
                int idx = i * 4;
                int b = data[idx] & 0xFF;
                int g = data[idx + 1] & 0xFF;
                int r = data[idx + 2] & 0xFF;
                int a = data[idx + 3] & 0xFF;
                pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }

            image.setRGB(0, 0, mat.cols(), mat.rows(), pixels, 0, mat.cols());
            return image;
        } else if (mat.channels() == 3) {
            // BGR -> convert to INT_RGB for high quality
            type = BufferedImage.TYPE_INT_RGB;
            byte[] data = new byte[mat.rows() * mat.cols() * 3];
            mat.get(0, 0, data);

            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
            int[] pixels = new int[mat.cols() * mat.rows()];

            // Convert BGR to RGB
            for (int i = 0; i < pixels.length; i++) {
                int idx = i * 3;
                int b = data[idx] & 0xFF;
                int g = data[idx + 1] & 0xFF;
                int r = data[idx + 2] & 0xFF;
                pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }

            image.setRGB(0, 0, mat.cols(), mat.rows(), pixels, 0, mat.cols());
            return image;
        } else {
            // Grayscale
            type = BufferedImage.TYPE_BYTE_GRAY;
            byte[] data = new byte[mat.rows() * mat.cols()];
            mat.get(0, 0, data);

            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
            image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
            return image;
        }
    }

}
