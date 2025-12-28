package id.rockierocker.image.exception;

import id.rockierocker.image.constant.ResponseCode;
import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final ResponseCode responseCode;

    public BadRequestException(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }
}
