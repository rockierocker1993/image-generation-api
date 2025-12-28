package id.rockierocker.image.constant;

import lombok.Getter;

@Getter
public enum CurveFittingMode {
    PIXEL("PIXEL","pixel"),
    POLYGON("POLYGON","polygon"),
    SPLINE("SPLINE","spline"),;
    private CurveFittingMode(String mode, String command) {
        this.mode = mode;
        this.command = command;
    }
    private final String mode;
    private final String command;
}
