package id.rockierocker.image.refinment;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;

public class OpenCVPNPRefinment {
    static {
        // Load OpenCV native binaries (org.openpnp:opencv)
        OpenCV.loadLocally();
    }
    // =========================
    // Public API
    // =========================

    /**
     * Refine ONNX mask (float 0..1) and apply as alpha to source image.
     */
    public BufferedImage refineAndApply(
            BufferedImage src,
            float[][] rawMask
    ) {
        Mat mask = maskToMat(rawMask);

        gaussianBlur(mask, 5);
        morphologyClose(mask, 1);
        featherAlpha(mask, 1.2);

        return applyMask(src, mask);
    }

    // =========================
    // Refinement Steps
    // =========================

    /** Convert float mask (0..1) to CV_8UC1 */
    private Mat maskToMat(float[][] mask) {
        int h = mask.length;
        int w = mask[0].length;

        Mat mat = new Mat(h, w, CvType.CV_8UC1);
        byte[] row = new byte[w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = Math.round(mask[y][x] * 255f);
                row[x] = (byte) Math.max(0, Math.min(255, v));
            }
            mat.put(y, 0, row);
        }
        return mat;
    }

    /** Soft edge (1–2 px) */
    private void gaussianBlur(Mat mask, int kernelSize) {
        if (kernelSize % 2 == 0) kernelSize++; // must be odd
        Imgproc.GaussianBlur(
                mask,
                mask,
                new Size(kernelSize, kernelSize),
                0
        );
    }

    /** Close small holes / clean outline */
    private void morphologyClose(Mat mask, int kernelSize) {
        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_ELLIPSE,
                new Size(kernelSize, kernelSize)
        );
        Imgproc.morphologyEx(
                mask,
                mask,
                Imgproc.MORPH_CLOSE,
                kernel
        );
    }

    /** Feather alpha using power curve */
    private void featherAlpha(Mat mask, double power) {
        mask.convertTo(mask, CvType.CV_32F, 1.0 / 255.0);
        Core.pow(mask, power, mask);
        mask.convertTo(mask, CvType.CV_8UC1, 255.0);
    }

    // =========================
    // Apply Mask
    // =========================

    private BufferedImage applyMask(BufferedImage src, Mat mask) {
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage out =
                new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        byte[] alpha = new byte[w * h];
        mask.get(0, 0, alpha);

        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int a = alpha[i++] & 0xFF;

                int rgba =
                        (a << 24) |
                                (rgb & 0x00FFFFFF);

                out.setRGB(x, y, rgba);
            }
        }
        return out;
    }
}
