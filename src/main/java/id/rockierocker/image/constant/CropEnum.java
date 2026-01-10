package id.rockierocker.image.constant;

import id.rockierocker.image.crop.OpenCVContourCrop;

public enum CropEnum {
    CROP_COUNTOUR(OpenCVContourCrop.class);
    CropEnum(Class<?> cropClass) {
        this.cropClass = cropClass;
    }
    public final Class<?> cropClass;
    public static CropEnum fromString(String name) {
        for (CropEnum p : CropEnum.values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown preprocess: " + name);
    }
}
