package id.rockierocker.image.rembg.constant;

public enum OnnxInputSize {
    INPUT_SIZE_320(320),
    INPUT_SIZE_512(512);
    private OnnxInputSize(Integer inputSize){
        this.inputSize = inputSize;
    }
    public final Integer inputSize;
}
