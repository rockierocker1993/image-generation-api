package id.rockierocker.image.dto.svgconversion;

import id.rockierocker.image.constant.ColorMode;
import id.rockierocker.image.constant.CurveFittingMode;
import id.rockierocker.image.constant.Hierarchical;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VtraceConversionDto {
    private ColorMode colorMode;
    private Hierarchical hierarchical;
    private Integer filterSpeckle;
    private Integer colorPrecision;
    private Integer gradientStep;
    private Integer cornerThreshold;
    private Double segmentLength;
    private Integer spliceThreshold;
    private CurveFittingMode curveFittingMode;
}
