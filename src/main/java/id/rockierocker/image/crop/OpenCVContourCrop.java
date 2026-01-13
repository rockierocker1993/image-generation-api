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

    @Override
    public List<BufferedImage> crop(BufferedImage inputImage) {
        log.info("Starting contour-based cropping using OpenCV...");
        List<BufferedImage> results = new ArrayList<>();
        // 1️⃣ Convert BufferedImage → Mat
        Mat src = bufferedImageToMat(inputImage);
        // 2️⃣ Grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        // 3️⃣ Adaptive Threshold (lebih stabil)
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255,
                Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        // 4️⃣ Morphology (gabungkan karakter yang pecah)
        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.morphologyEx(binary, binary,
                Imgproc.MORPH_CLOSE, kernel);
        // 5️⃣ Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(binary, contours, new Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);
        // 6️⃣ Sort contour: kiri → kanan, atas → bawah
        contours.sort(Comparator.comparingInt(c -> {
            Rect r = Imgproc.boundingRect(c);
            return r.y * 10000 + r.x;
        }));
        // 7️⃣ Crop
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            // Filter noise
            if (rect.area() < 1500) continue;
            Mat cropped = new Mat(src, rect);
            results.add(matToBufferedImage(cropped));
        }
        return results;
    }

    private Mat bufferedImageToMat(BufferedImage bi) {
        // Convert to BGR format if needed to ensure compatibility with OpenCV
        BufferedImage convertedImage = bi;
        if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            log.debug("Converting BufferedImage from type {} to TYPE_3BYTE_BGR", bi.getType());
            convertedImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            convertedImage.getGraphics().drawImage(bi, 0, 0, null);
        }

        Mat mat = new Mat(convertedImage.getHeight(), convertedImage.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (mat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }

        byte[] data = new byte[mat.rows() * mat.cols() * mat.channels()];
        mat.get(0, 0, data);

        BufferedImage image = new BufferedImage(
                mat.cols(), mat.rows(), type);
        image.getRaster().setDataElements(0, 0,
                mat.cols(), mat.rows(), data);
        return image;
    }



}
