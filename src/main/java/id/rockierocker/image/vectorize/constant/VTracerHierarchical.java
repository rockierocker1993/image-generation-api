package id.rockierocker.image.vectorize.constant;

import lombok.Getter;
/**
 * Enum for hierarchical modes.
 * Only applies to color mode, ColorMode.COLOR
 */
@Getter
public enum VTracerHierarchical {
    STACKED("STACKED", "stacked"),
    CUTOUT("CUTOUT","cutout");
    private VTracerHierarchical(String mode, String command) {
        this.mode = mode;
        this.command = command;
    }
    private final String mode;
    private final String command;
}
