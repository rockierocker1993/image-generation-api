package id.rockierocker.image.rembg.constant;

public enum OnnxInputSize {
    INPUT_SIZE_320(320),
    INPUT_SIZE_512(512),
    INPUT_SIZE_1024(1024);
    private OnnxInputSize(Integer inputSize){
        this.inputSize = inputSize;
    }
    public final Integer inputSize;
    public static OnnxInputSize fromString(String name) {
        for (OnnxInputSize p : OnnxInputSize.values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown OnnxInputSize: " + name);
    }
}
