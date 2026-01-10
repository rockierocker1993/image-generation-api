package id.rockierocker.image.crop;

import java.awt.image.BufferedImage;
import java.util.List;

public interface Crop {

    List<BufferedImage> crop(BufferedImage inputImage) throws Exception;

}
