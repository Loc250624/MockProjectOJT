package com.ojtsu26.elearning.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    
    // System Errors
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_ERROR(400, HttpStatus.BAD_REQUEST, "Validation error"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "Unauthorized access"),
    ACCESS_DENIED(403, HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "Resource not found"),

    // Business Errors
    USER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "User does not exist"),
    USER_ALREADY_EXISTS(409, HttpStatus.CONFLICT, "User already exists"),
    CANNOT_MODIFY_OWN_ACCOUNT(400, HttpStatus.BAD_REQUEST, "Admin cannot modify their own account"),
    CANNOT_DELETE_OWN_ACCOUNT(400, HttpStatus.BAD_REQUEST, "Admin cannot soft-delete their own account"),
    USER_ALREADY_BLOCKED(409, HttpStatus.CONFLICT, "User is already blocked"),
    USER_NOT_BLOCKED(409, HttpStatus.CONFLICT, "User is not blocked"),
    USER_ALREADY_DELETED(409, HttpStatus.CONFLICT, "User account is already soft-deleted");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
