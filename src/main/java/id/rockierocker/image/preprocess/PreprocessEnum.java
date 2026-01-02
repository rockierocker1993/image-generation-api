package id.rockierocker.image.preprocess;

import id.rockierocker.image.preprocess.model.PreprocessConfig;

import java.awt.image.BufferedImage;

public enum PreprocessEnum {
    K_MEANS_QUANTIZATION(KMeansQuantization.class),
    ADJUST_CONTRAST(AdjustContrast.class),
    SHARPEN(Sharpen.class);

    PreprocessEnum(Class<?> PreprocessClass) {
        this.PreprocessClass = PreprocessClass;
    }

    public final Class<?> PreprocessClass;
    public static PreprocessEnum fromString(String name) {
        for (PreprocessEnum p : PreprocessEnum.values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown preprocess: " + name);
    }
}
