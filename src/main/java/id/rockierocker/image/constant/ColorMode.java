package id.rockierocker.image.constant;

import lombok.Getter;

@Getter
public enum ColorMode {
    BW("BW", "bw"),
    COLOR("COLOR","color");
    private ColorMode(String mode, String command) {
        this.mode = mode;
        this.command = command;
    }
    private final String mode;
    private final String command;
}
