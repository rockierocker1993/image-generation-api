package id.rockierocker.image.dto.svgconversion;

import id.rockierocker.image.vectorize.constant.VTracerColorMode;
import id.rockierocker.image.vectorize.constant.VTracerCurveFittingMode;
import id.rockierocker.image.vectorize.constant.VTracerHierarchical;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VtraceConversionDto {
    private VTracerColorMode colorMode;
    private VTracerHierarchical hierarchical;
    private Integer filterSpeckle;
    private Integer colorPrecision;
    private Integer gradientStep;
    private Integer cornerThreshold;
    private Double segmentLength;
    private Integer spliceThreshold;
    private VTracerCurveFittingMode curveFittingMode;
}
