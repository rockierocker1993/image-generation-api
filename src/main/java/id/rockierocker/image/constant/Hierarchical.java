package id.rockierocker.image.constant;

import lombok.Getter;
/**
 * Enum for hierarchical modes.
 * Only applies to color mode, ColorMode.COLOR
 */
@Getter
public enum Hierarchical {
    STACKED("STACKED", "stacked"),
    CUTOUT("CUTOUT","cutout");
    private Hierarchical(String mode, String command) {
        this.mode = mode;
        this.command = command;
    }
    private final String mode;
    private final String command;
}
