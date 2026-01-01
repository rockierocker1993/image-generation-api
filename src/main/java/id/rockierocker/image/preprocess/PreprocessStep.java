package id.rockierocker.image.preprocess;

import id.rockierocker.image.preprocess.model.PreprocessConfig;

import java.awt.image.BufferedImage;

public enum PreprocessStep {
    K_MEANS_QUANTIZATION(KMeansQuantization.class),
    ADJUST_CONTRAST(AdjustContrast.class),
    SHARPEN(Sharpen.class);

    PreprocessStep(Class<?> PreprocessClass) {
        this.PreprocessClass = PreprocessClass;
    }

    public final Class<?> PreprocessClass;

    public static BufferedImage process(String name, BufferedImage inputImage, PreprocessConfig preprocessConfig, RuntimeException runtimeException) {
        try {
            PreprocessStep preprocessStep = null;
            for (PreprocessStep step : PreprocessStep.values()) {
                if (step.name().equalsIgnoreCase(name)) {
                    preprocessStep = step;
                    break;
                }
            }
            if (preprocessStep != null) {
                ImagePreprocess imagePreprocess = (ImagePreprocess) preprocessStep.PreprocessClass.getDeclaredConstructor().newInstance();
                return imagePreprocess.process(inputImage, preprocessConfig);
            }

        } catch (Exception e) {
            throw runtimeException;
        }
        return null;
    }
}
