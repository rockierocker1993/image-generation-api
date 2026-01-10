package id.rockierocker.image.constant;

import id.rockierocker.image.crop.OpenCVContourCrop;
import id.rockierocker.image.rembg.ByHexCodeRembg;
import id.rockierocker.image.rembg.OnnxRembg;
import id.rockierocker.image.rembg.OpenCVRembg;

public enum RembgEnum {
    BY_HEX_CODE_REMBG(ByHexCodeRembg.class),
    ONNX_REMBG(OnnxRembg.class),
    OPEN_CV(OpenCVRembg .class);
    RembgEnum(Class<?> rembgClass) {
        this.rembgClass = rembgClass;
    }
    public final Class<?> rembgClass;
    public static RembgEnum fromString(String name) {
        for (RembgEnum p : RembgEnum.values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown preprocess: " + name);
    }
}
