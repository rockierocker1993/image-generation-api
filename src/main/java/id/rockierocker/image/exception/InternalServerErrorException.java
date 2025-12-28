package id.rockierocker.image.exception;

import id.rockierocker.image.constant.ResponseCode;
import lombok.Getter;

@Getter
public class InternalServerErrorException extends RuntimeException {

    private final ResponseCode responseCode;

    public InternalServerErrorException(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }
}
