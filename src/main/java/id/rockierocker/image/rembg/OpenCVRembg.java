package id.rockierocker.image.rembg;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class OpenCVRembg implements Rembg {

    private Map<String, Object> config;

    static {
        try {
            // Load OpenCV native library
            nu.pattern.OpenCV.loadLocally();
            log.info("OpenCV native library loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load OpenCV native library", e);
            throw new RuntimeException("OpenCV native library not available", e);
        }
    }

    @Override
    public BufferedImage removeBackground(BufferedImage inputImage) throws Exception {
        if (config == null) {
            throw new IllegalAccessException("OpenCV Rembg not configured yet");
        }

        return process(inputImage);
    }

    @Override
    public String getName() {
        return "OpenCVRembg";
    }

    @Override
    public void configMap(Map<String, Object> config) {
        this.config = config;
    }

    private BufferedImage process(BufferedImage inputImage) throws Exception {
        log.info("Starting OpenCV background removal...");

        // Read input image
        log.info("Image size: {}x{}", inputImage.getWidth(), inputImage.getHeight());

        // Convert BufferedImage to OpenCV Mat
        Mat src = bufferedImageToMat(inputImage);

        // Apply background removal using multiple techniques
        String method = (String) config.getOrDefault("method", "auto");

        // Auto-detect best method if set to "auto"
        if ("auto".equalsIgnoreCase(method)) {
            method = detectBestMethod(src);
            log.info("Auto-detected best method: {}", method);
        }

        Mat result;

        switch (method.toLowerCase()) {
            case "grabcut":
                result = removeBackgroundGrabCut(src);
                break;
            case "contour":
                result = removeBackgroundContour(src);
                break;
            case "contour-holes":
                result = removeBackgroundContourWithHoles(src);
                break;
            case "threshold":
                result = removeBackgroundThreshold(src);
                break;
            default:
                result = removeBackgroundGrabCut(src);
        }

        // Convert back to BufferedImage with alpha channel
        BufferedImage output = matToBufferedImage(result);

        // Clean up
        src.release();
        result.release();

        log.info("Successfully removed background using OpenCV method: {}", method);
        return output;
    }

    /**
     * Auto-detect the best method based on image characteristics
     */
    private String detectBestMethod(Mat src) {
        log.info("Analyzing image to detect best method...");

        // Convert to HSV for analysis
        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        // Calculate background uniformity by sampling edges
        double uniformity = calculateBackgroundUniformity(hsv);
        log.info("Background uniformity score: {}", uniformity);

        // Calculate edge strength
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);
        double edgeRatio = Core.countNonZero(edges) / (double)(edges.rows() * edges.cols());
        log.info("Edge ratio: {}", edgeRatio);

        // Decision logic
        String method;

        if (uniformity > 0.85) {
            // High uniformity = uniform background (likely white/solid color)
            method = "threshold";
            log.info("Detected uniform background -> using threshold");
        } else if (edgeRatio > 0.05) {
            // High edge ratio = clear object boundaries
            method = "contour";
            log.info("Detected clear edges -> using contour");
        } else {
            // Complex background or unclear edges
            method = "grabcut";
            log.info("Detected complex background -> using grabcut");
        }

        // Clean up
        hsv.release();
        gray.release();
        edges.release();

        return method;
    }

    /**
     * Calculate background uniformity by analyzing edge pixels
     */
    private double calculateBackgroundUniformity(Mat hsv) {
        // Sample pixels from the edges (likely background)
        int rows = hsv.rows();
        int cols = hsv.cols();

        List<Double> brightnessValues = new ArrayList<>();
        List<Double> saturationValues = new ArrayList<>();

        // Sample top edge
        for (int x = 0; x < cols; x += 5) {
            double[] pixel = hsv.get(0, x);
            if (pixel != null) {
                saturationValues.add(pixel[1]);
                brightnessValues.add(pixel[2]);
            }
        }

        // Sample bottom edge
        for (int x = 0; x < cols; x += 5) {
            double[] pixel = hsv.get(rows - 1, x);
            if (pixel != null) {
                saturationValues.add(pixel[1]);
                brightnessValues.add(pixel[2]);
            }
        }

        // Sample left edge
        for (int y = 0; y < rows; y += 5) {
            double[] pixel = hsv.get(y, 0);
            if (pixel != null) {
                saturationValues.add(pixel[1]);
                brightnessValues.add(pixel[2]);
            }
        }

        // Sample right edge
        for (int y = 0; y < rows; y += 5) {
            double[] pixel = hsv.get(y, cols - 1);
            if (pixel != null) {
                saturationValues.add(pixel[1]);
                brightnessValues.add(pixel[2]);
            }
        }

        // Calculate standard deviation
        double avgBrightness = brightnessValues.stream().mapToDouble(d -> d).average().orElse(0);
        double avgSaturation = saturationValues.stream().mapToDouble(d -> d).average().orElse(0);

        double stdDevBrightness = Math.sqrt(
            brightnessValues.stream()
                .mapToDouble(d -> Math.pow(d - avgBrightness, 2))
                .average().orElse(0)
        );

        double stdDevSaturation = Math.sqrt(
            saturationValues.stream()
                .mapToDouble(d -> Math.pow(d - avgSaturation, 2))
                .average().orElse(0)
        );

        // Uniformity score: lower std dev = more uniform (0-1 scale)
        // High brightness (>200) + low saturation (<30) + low std dev = uniform white background
        double brightnessUniformity = 1.0 - Math.min(stdDevBrightness / 128.0, 1.0);
        double saturationUniformity = 1.0 - Math.min(stdDevSaturation / 128.0, 1.0);
        boolean isLightBackground = avgBrightness > 200 && avgSaturation < 30;

        double uniformity = (brightnessUniformity + saturationUniformity) / 2.0;
        if (isLightBackground) {
            uniformity *= 1.2; // Boost score for light backgrounds
        }

        return Math.min(uniformity, 1.0);
    }

    /**
     * Remove background using GrabCut algorithm (best for complex backgrounds)
     */
    private Mat removeBackgroundGrabCut(Mat src) {
        log.info("Using GrabCut algorithm...");

        Mat mask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();

        // Define rectangle around the foreground (assume center with margin)
        int margin = 10;
        Rect rect = new Rect(
            margin,
            margin,
            src.cols() - 2 * margin,
            src.rows() - 2 * margin
        );

        // Run GrabCut algorithm
        Imgproc.grabCut(
            src,
            mask,
            rect,
            bgModel,
            fgModel,
            5,
            Imgproc.GC_INIT_WITH_RECT
        );

        // Create mask where 0 and 2 are background, 1 and 3 are foreground
        Mat mask2 = new Mat();
        Core.compare(mask, new Scalar(Imgproc.GC_PR_FGD), mask2, Core.CMP_EQ);
        Mat mask3 = new Mat();
        Core.compare(mask, new Scalar(Imgproc.GC_FGD), mask3, Core.CMP_EQ);
        Mat finalMask = new Mat();
        Core.bitwise_or(mask2, mask3, finalMask);

        // Apply mask to create RGBA image
        Mat result = applyAlphaMask(src, finalMask);

        // Clean up
        mask.release();
        bgModel.release();
        fgModel.release();
        mask2.release();
        mask3.release();
        finalMask.release();

        return result;
    }

    /**
     * Remove background using contour detection (good for objects with clear edges)
     */
    private Mat removeBackgroundContour(Mat src) {
        log.info("Using Contour detection...");

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply Gaussian blur
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        // Edge detection
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);

        // Dilate to connect edges
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.dilate(edges, edges, kernel);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Create mask and draw largest contour
        Mat mask = Mat.zeros(src.size(), CvType.CV_8UC1);
        if (!contours.isEmpty()) {
            // Find largest contour
            double maxArea = 0;
            int maxIndex = 0;
            for (int i = 0; i < contours.size(); i++) {
                double area = Imgproc.contourArea(contours.get(i));
                if (area > maxArea) {
                    maxArea = area;
                    maxIndex = i;
                }
            }

            // Fill the largest contour
            Imgproc.drawContours(mask, contours, maxIndex, new Scalar(255), -1);
            log.info("Drew largest contour at index {} with area {}", maxIndex, maxArea);
        }

        // Apply mask
        Mat result = applyAlphaMask(src, mask);

        // Clean up
        gray.release();
        edges.release();
        kernel.release();
        hierarchy.release();
        mask.release();

        return result;
    }

    /**
     * Remove background using contour detection with hole support
     * (good for objects with clear edges that have inner backgrounds/holes)
     */
    private Mat removeBackgroundContourWithHoles(Mat src) {
        log.info("Using Contour detection with hole support (optimized)...");

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply bilateral filter to reduce noise while preserving edges
        Mat filtered = new Mat();
        Imgproc.bilateralFilter(gray, filtered, 9, 75, 75);

        // Use adaptive threshold for better edge detection in varying lighting
        Mat binary = new Mat();
        Imgproc.adaptiveThreshold(filtered, binary, 255,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 11, 2);

        // Combine with Canny for better edge detection
        Mat edges = new Mat();
        Imgproc.Canny(filtered, edges, 30, 90, 3, true);

        // Merge binary and edges for comprehensive edge map
        Mat combined = new Mat();
        Core.bitwise_or(binary, edges, combined);

        // Morphological operations to connect edges and fill small gaps
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(combined, combined, Imgproc.MORPH_CLOSE, kernel, new Point(-1, -1), 2);

        // Find contours with full hierarchy (RETR_TREE for multi-level holes)
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(combined, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Create mask
        Mat mask = Mat.zeros(src.size(), CvType.CV_8UC1);

        if (!contours.isEmpty()) {
            double imageArea = src.rows() * src.cols();
            double minAreaThreshold = imageArea * 0.01; // Ignore contours < 1% of image
            double maxAreaThreshold = imageArea * 0.95; // Ignore contours > 95% of image

            // Find largest valid outer contour
            double maxArea = 0;
            int largestOuterIdx = -1;

            for (int i = 0; i < contours.size(); i++) {
                double[] h = hierarchy.get(0, i);
                // h[3] is parent: -1 means it's a top-level outer contour
                if (h[3] == -1) {
                    double area = Imgproc.contourArea(contours.get(i));
                    // Filter by area to avoid noise and full-image contours
                    if (area > minAreaThreshold && area < maxAreaThreshold && area > maxArea) {
                        // Additional check: ensure contour is not at image border
                        Rect bounds = Imgproc.boundingRect(contours.get(i));
                        boolean atBorder = bounds.x <= 2 || bounds.y <= 2 ||
                                         bounds.x + bounds.width >= src.cols() - 2 ||
                                         bounds.y + bounds.height >= src.rows() - 2;

                        if (!atBorder) {
                            maxArea = area;
                            largestOuterIdx = i;
                        }
                    }
                }
            }

            if (largestOuterIdx >= 0) {
                // Draw outer contour with smooth approximation
                MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(largestOuterIdx).toArray());
                MatOfPoint2f approx = new MatOfPoint2f();
                double epsilon = 0.001 * Imgproc.arcLength(contour2f, true);
                Imgproc.approxPolyDP(contour2f, approx, epsilon, true);

                MatOfPoint approxContour = new MatOfPoint(approx.toArray());
                List<MatOfPoint> smoothContours = new ArrayList<>();
                smoothContours.add(approxContour);

                // Fill outer contour (white = keep object)
                Imgproc.drawContours(mask, smoothContours, 0, new Scalar(255), -1);
                log.info("Drew largest outer contour at index {} with area {:.2f} ({:.1f}% of image)",
                    largestOuterIdx, maxArea, (maxArea / imageArea) * 100);

                // Process holes recursively (any child of the outer contour)
                int holesCount = processHoles(mask, contours, hierarchy, largestOuterIdx, minAreaThreshold);

                if (holesCount > 0) {
                    log.info("Removed {} inner backgrounds (holes)", holesCount);
                }

                // Clean up temporary objects
                contour2f.release();
                approx.release();
                approxContour.release();
            } else {
                log.warn("No valid outer contour found, using fallback method");
                // Fallback: use threshold method
                filtered.release();
                binary.release();
                edges.release();
                combined.release();
                kernel.release();
                hierarchy.release();
                gray.release();
                mask.release();
                return removeBackgroundThreshold(src);
            }
        }

        // Post-process mask: smooth edges and remove small noise
        Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel2, new Point(-1, -1), 1);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel2, new Point(-1, -1), 1);

        // Gaussian blur on mask for smoother alpha edges
        Imgproc.GaussianBlur(mask, mask, new Size(3, 3), 0);

        // Apply mask
        Mat result = applyAlphaMask(src, mask);

        // Clean up
        gray.release();
        filtered.release();
        binary.release();
        edges.release();
        combined.release();
        kernel.release();
        kernel2.release();
        hierarchy.release();
        mask.release();

        return result;
    }

    /**
     * Recursively process holes (nested contours)
     */
    private int processHoles(Mat mask, List<MatOfPoint> contours, Mat hierarchy, int parentIdx, double minAreaThreshold) {
        int count = 0;

        for (int i = 0; i < contours.size(); i++) {
            double[] h = hierarchy.get(0, i);
            // h[3] is parent index
            if (h[3] == parentIdx) {
                double area = Imgproc.contourArea(contours.get(i));
                // Only process holes that are significant enough
                if (area > minAreaThreshold) {
                    // Draw hole (black = remove)
                    Imgproc.drawContours(mask, contours, i, new Scalar(0), -1);
                    count++;

                    // Recursively process any nested contours inside this hole
                    // (those would be objects inside the hole that should be kept)
                    int nestedCount = processNestedObjects(mask, contours, hierarchy, i, minAreaThreshold);
                    if (nestedCount > 0) {
                        log.debug("Found {} nested objects inside hole {}", nestedCount, i);
                    }
                }
            }
        }

        return count;
    }

    /**
     * Process nested objects inside holes (should be kept)
     */
    private int processNestedObjects(Mat mask, List<MatOfPoint> contours, Mat hierarchy, int parentIdx, double minAreaThreshold) {
        int count = 0;

        for (int i = 0; i < contours.size(); i++) {
            double[] h = hierarchy.get(0, i);
            // h[3] is parent index
            if (h[3] == parentIdx) {
                double area = Imgproc.contourArea(contours.get(i));
                // Only process objects that are significant enough
                if (area > minAreaThreshold) {
                    // Draw nested object (white = keep)
                    Imgproc.drawContours(mask, contours, i, new Scalar(255), -1);
                    count++;

                    // Recursively process holes in nested objects
                    processHoles(mask, contours, hierarchy, i, minAreaThreshold);
                }
            }
        }

        return count;
    }

    /**
     * Remove background using simple thresholding (good for uniform backgrounds)
     */
    private Mat removeBackgroundThreshold(Mat src) {
        log.info("Using Threshold method...");

        // Convert to HSV for better color segmentation
        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        // Create mask for white/bright background
        Mat mask = new Mat();
        Core.inRange(hsv, new Scalar(0, 0, 200), new Scalar(180, 30, 255), mask);

        // Invert mask (we want to keep the object, not the background)
        Core.bitwise_not(mask, mask);

        // Morphological operations to clean up
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

        // Apply mask
        Mat result = applyAlphaMask(src, mask);

        // Clean up
        hsv.release();
        mask.release();
        kernel.release();

        return result;
    }

    /**
     * Apply alpha mask to source image
     */
    private Mat applyAlphaMask(Mat src, Mat mask) {
        // Convert mask to same size as source
        Mat mask8U = new Mat();
        if (mask.type() != CvType.CV_8UC1) {
            mask.convertTo(mask8U, CvType.CV_8UC1);
        } else {
            mask8U = mask.clone();
        }

        // Resize mask if needed
        if (mask8U.size().width != src.size().width || mask8U.size().height != src.size().height) {
            Imgproc.resize(mask8U, mask8U, src.size());
        }

        // Create BGRA image
        Mat bgra = new Mat();
        Imgproc.cvtColor(src, bgra, Imgproc.COLOR_BGR2BGRA);

        // Split channels
        List<Mat> channels = new ArrayList<>();
        Core.split(bgra, channels);

        // Replace alpha channel with mask
        mask8U.copyTo(channels.get(3));

        // Merge channels back
        Mat result = new Mat();
        Core.merge(channels, result);

        // Clean up
        mask8U.release();
        bgra.release();
        for (Mat ch : channels) {
            ch.release();
        }

        return result;
    }

    /**
     * Convert BufferedImage to OpenCV Mat
     */
    private Mat bufferedImageToMat(BufferedImage image) {
        // Convert to BGR color space for OpenCV
        BufferedImage convertedImage = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_3BYTE_BGR
        );
        convertedImage.getGraphics().drawImage(image, 0, 0, null);

        byte[] pixels = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);

        return mat;
    }

    /**
     * Convert OpenCV Mat to BufferedImage
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_INT_ARGB;

        // Convert Mat to BGRA if needed
        Mat bgra = new Mat();
        if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, bgra, Imgproc.COLOR_BGR2BGRA);
        } else if (mat.channels() == 4) {
            bgra = mat.clone();
        } else {
            Imgproc.cvtColor(mat, bgra, Imgproc.COLOR_GRAY2BGRA);
        }

        byte[] data = new byte[bgra.rows() * bgra.cols() * (int) bgra.elemSize()];
        bgra.get(0, 0, data);

        BufferedImage image = new BufferedImage(bgra.cols(), bgra.rows(), type);

        // Convert BGRA to ARGB for BufferedImage
        int[] pixels = new int[bgra.cols() * bgra.rows()];
        for (int i = 0; i < pixels.length; i++) {
            int idx = i * 4;
            int b = data[idx] & 0xFF;
            int g = data[idx + 1] & 0xFF;
            int r = data[idx + 2] & 0xFF;
            int a = data[idx + 3] & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        image.setRGB(0, 0, bgra.cols(), bgra.rows(), pixels, 0, bgra.cols());

        bgra.release();
        return image;
    }
//
//    public static void main(String[] args) throws Exception {
//        OpenCVRembg openCVRembg = new OpenCVRembg();
//        openCVRembg.configMap(Map.of(
//                "method", "contour"  // "auto", "contour", "threshold", or "grabcut"
//        ));
//        File testFile = new File("./data-test/rembg/test4.png");
//        InputStream inputImage = CommonUtil.toInputStream(testFile, new RuntimeException());
//        byte[] outputImage = openCVRembg.removeBackground(inputImage);
//
//        File outputFile = new File(testFile.getAbsolutePath() + ".result-opencv.png");
//        Files.write(outputFile.toPath(), outputImage);
//    }
}
