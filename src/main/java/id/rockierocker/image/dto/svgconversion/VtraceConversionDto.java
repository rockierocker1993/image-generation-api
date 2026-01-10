package id.rockierocker.image.dto.svgconversion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.awt.image.BufferedImage;
import java.io.File;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class VtraceConversionDto {
    private String preprocessStepCode;
    private String vtraceConfigCode;
    private String rembgConfigCode;
    @JsonIgnore
    private String originalFilename;
    @JsonIgnore
    private File inputFile;
    @JsonIgnore
    private String ext;
    @JsonIgnore
    private byte[] inputBytes;
    @JsonIgnore
    private BufferedImage inputBufferedImage;
}
