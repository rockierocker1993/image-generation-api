package id.rockierocker.image.exception;


import id.rockierocker.image.constant.ResponseCode;
import id.rockierocker.image.dto.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {
        ResponseCode responseCode = ex.getResponseCode();
        return ResponseEntity.badRequest().body(
                BaseResponse.builder()
                        .status(false)
                        .responseCode(responseCode.getCode())
                        .titleEn(responseCode.getTitleEn())
                        .titleId(responseCode.getTitleId())
                        .messageEn(responseCode.getMessageEn())
                        .messageId(responseCode.getMessageId())
                        .build()
        );
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<BaseResponse> handleGeneralException(
            InternalServerErrorException ex,
            HttpServletRequest request) {

        ResponseCode responseCode = ex.getResponseCode();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse.builder()
                .status(false)
                .responseCode(responseCode.getCode())
                .titleEn(responseCode.getTitleEn())
                .titleId(responseCode.getTitleId())
                .messageEn(responseCode.getMessageEn())
                .messageId(responseCode.getMessageId())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        ResponseCode responseCode = ResponseCode.UKNOWN_ERROR;

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse.builder()
                .status(false)
                .responseCode(responseCode.getCode())
                .titleEn(responseCode.getTitleEn())
                .titleId(responseCode.getTitleId())
                .messageEn(responseCode.getMessageEn())
                .messageId(responseCode.getMessageId())
                .build());
    }
}
