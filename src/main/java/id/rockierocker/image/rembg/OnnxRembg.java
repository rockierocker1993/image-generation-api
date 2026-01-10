package id.rockierocker.image.rembg;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.TensorInfo;
import id.rockierocker.image.refinment.OpenCVPNPRefinment;
import id.rockierocker.image.rembg.constant.OnnxInputSize;
import id.rockierocker.image.util.ImageUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.*;

@NoArgsConstructor
@Slf4j
public class OnnxRembg implements Rembg {

    // =========================
    // CONFIG
    // =========================
    static final int K_COLORS = 5;        // recommended 4–6
    static final float CONTRAST = 1.2f;   // 1.1 – 1.3
    static final int ITERATIONS = 10;

    static final float[][] SHARPEN_KERNEL = {
            { 0, -1,  0 },
            { -1,  5, -1 },
            { 0, -1,  0 }
    };

    private Map<String, Object> config;
    OpenCVPNPRefinment openCVPNPRefinment = new OpenCVPNPRefinment();

    @Override
    public String getName() {
        return "OnnxRembg";
    }

    @Override
    public void configMap(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public BufferedImage removeBackground(BufferedImage inputImage) throws Exception {
        if (Objects.isNull(config))
            throw new IllegalAccessException("ONNX Rembg not configured yet");

        return process(inputImage);
    }

    /* Process the input image to remove background using ONNX model */
    private BufferedImage process(BufferedImage inputImage) throws Exception {
        String modelPath = (String) config.get("onnxModelPath");
        log.info("Starting onnx background removal using {} model...", modelPath);

        OnnxInputSize onnxInputSize = OnnxInputSize.fromString(config.getOrDefault("onnxInputSize", "INPUT_SIZE_320").toString());
        Integer configuredInputSize = onnxInputSize.inputSize;
        log.info("Configured ONNX input size: {}", onnxInputSize);

        OrtEnvironment env = OrtEnvironment.getEnvironment();
        OrtSession session = env.createSession(modelPath);
        try {
            // Inspect model input shape and adapt if model expects a different size
            String inputName = session.getInputNames().iterator().next();
            NodeInfo nodeInfo = session.getInputInfo().get(inputName);
            long[] modelShape = null;
            if (nodeInfo != null && nodeInfo.getInfo() instanceof TensorInfo) {
                TensorInfo tinfo = (TensorInfo) nodeInfo.getInfo();
                modelShape = tinfo.getShape();
            }
            log.info("Model input name='{}' shape={}", inputName, Arrays.toString(modelShape));

            // Determine target W/H to resize image to. Default to configuredInputSize.
            int targetW = configuredInputSize;
            int targetH = configuredInputSize;
            if (modelShape != null && modelShape.length >= 4) {
                long maybeN = modelShape[0];
                long maybeC = modelShape[1];
                long maybeH = modelShape[2];
                long maybeW = modelShape[3];
                // If model provides positive dims, use them. If any dimension is <=0, fall back to configured size.
                if (maybeH > 0) targetH = (int) maybeH;
                if (maybeW > 0) targetW = (int) maybeW;
            }

            log.info("resizing input image to {}x{}...", targetW + "", targetH + "");
            BufferedImage resized = ImageUtil.resize(inputImage, targetW, targetH);
            log.info("converting image to tensor...");
            float[] tensorData = imageToTensor(resized, targetW, targetH);
            log.info("running inference to get mask model...");
            float[][] mask = runInference(env, session, tensorData, targetW, targetH);
            log.info("resizing mask to original image size...");
            float[][] resizeMaskToOriginalSize = resizeMask(mask, inputImage.getWidth(), inputImage.getHeight());
            log.info("applying mask to original image...");
            BufferedImage applyMask = openCVPNPRefinment.refineAndApply(inputImage, resizeMaskToOriginalSize);
            //BufferedImage applyMask = applyMask(original, resizeMaskToOriginalSize);
            log.info("successfully removed background from image");
            return applyMask;
        } finally {
            session.close();
            env.close();
        }
    }

    /* Convert BufferedImage to float tensor with shape [1, 3, H, W] */
    private float[] imageToTensor(BufferedImage img, int W, int H) {
        float[] data = new float[1 * 3 * W * H];

        int idxR = 0;
        int idxG = W * H;
        int idxB = 2 * W * H;

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y);

                float r = ((rgb >> 16) & 0xFF) / 255f;
                float g = ((rgb >> 8) & 0xFF) / 255f;
                float b = (rgb & 0xFF) / 255f;

                data[idxR++] = (r - 0.5f) / 0.5f;
                data[idxG++] = (g - 0.5f) / 0.5f;
                data[idxB++] = (b - 0.5f) / 0.5f;
            }
        }
        return data;
    }

    /* Run inference on the ONNX model and return the output mask */
    private float[][] runInference(
            OrtEnvironment env,
            OrtSession session,
            float[] input,
            int W,
            int H
    ) throws Exception {

        OnnxTensor tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(input),
                new long[]{1, 3, H, W}
        );

        String inputName = session.getInputNames().iterator().next();

        OrtSession.Result result = session.run(
                Map.of(inputName, tensor)
        );

        float[][][][] output =
                (float[][][][]) result.get(0).getValue();

        return output[0][0]; // H x W mask (depending on model)
    }

    private float[][] resizeMask(
            float[][] mask,
            int targetW,
            int targetH
    ) {
        int srcH = mask.length;
        int srcW = mask[0].length;

        float[][] resized = new float[targetH][targetW];

        for (int y = 0; y < targetH; y++) {
            float gy = ((float) y / (targetH - 1)) * (srcH - 1);
            int y0 = (int) gy;
            int y1 = Math.min(y0 + 1, srcH - 1);
            float dy = gy - y0;

            for (int x = 0; x < targetW; x++) {
                float gx = ((float) x / (targetW - 1)) * (srcW - 1);
                int x0 = (int) gx;
                int x1 = Math.min(x0 + 1, srcW - 1);
                float dx = gx - x0;

                float v =
                        mask[y0][x0] * (1 - dx) * (1 - dy) +
                                mask[y0][x1] * dx * (1 - dy) +
                                mask[y1][x0] * (1 - dx) * dy +
                                mask[y1][x1] * dx * dy;

                resized[y][x] = v;
            }
        }
        return resized;
    }


    private BufferedImage applyMask(
            BufferedImage original,
            float[][] mask
    ) {
        int W = original.getWidth();
        int H = original.getHeight();

        BufferedImage out =
                new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = original.getRGB(x, y);

                int alpha = (int) (mask[y][x] * 255);
                alpha = alpha > 128 ? 255 : 0; // threshold

                int rgba =
                        (alpha << 24) |
                                (rgb & 0x00FFFFFF);

                out.setRGB(x, y, rgba);
            }
        }
        return out;
    }
}
