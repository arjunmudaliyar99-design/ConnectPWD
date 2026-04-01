package org.connectpwd.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public AppException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public static AppException notFound(ErrorCode code, String message) {
        return new AppException(code, message, HttpStatus.NOT_FOUND);
    }

    public static AppException conflict(ErrorCode code, String message) {
        return new AppException(code, message, HttpStatus.CONFLICT);
    }

    public static AppException unauthorized(ErrorCode code, String message) {
        return new AppException(code, message, HttpStatus.UNAUTHORIZED);
    }

    public static AppException forbidden(ErrorCode code, String message) {
        return new AppException(code, message, HttpStatus.FORBIDDEN);
    }

    public static AppException badRequest(ErrorCode code, String message) {
        return new AppException(code, message, HttpStatus.BAD_REQUEST);
    }
}
