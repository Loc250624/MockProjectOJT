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
    ASSESSMENT_CONTENT_ALREADY_EXISTS(409, HttpStatus.CONFLICT, "Assessment content already exists"),
    EMAIL_ALREADY_EXISTS(409, HttpStatus.CONFLICT, "Email already exists"),
    COURSE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "Course does not exist"),
    COURSE_UNAVAILABLE(409, HttpStatus.CONFLICT, "Course is not available"),
    ENROLLMENT_NOT_FOUND(404, HttpStatus.NOT_FOUND, "Enrollment does not exist"),
    ENROLLMENT_ACCESS_DENIED(403, HttpStatus.FORBIDDEN, "Enrollment access denied"),
    CERTIFICATE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "Certificate does not exist"),
    CERTIFICATE_ACCESS_DENIED(403, HttpStatus.FORBIDDEN, "Certificate access denied"),
    CERTIFICATE_NOT_ELIGIBLE(409, HttpStatus.CONFLICT, "Certificate is not eligible"),
    CERTIFICATE_ALREADY_REVOKED(409, HttpStatus.CONFLICT, "Certificate is already revoked"),
    CERTIFICATE_GENERATION_FAILED(500, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate certificate"),
    INVALID_VERIFICATION_CODE(400, HttpStatus.BAD_REQUEST, "Invalid verification code"),
    ENROLLMENT_NOT_ALLOWED(400, HttpStatus.BAD_REQUEST, "Enrollment is not allowed"),
    PAYMENT_REQUIRED(402, HttpStatus.PAYMENT_REQUIRED, "Payment is required"),
    PAYMENT_NOT_VERIFIED(400, HttpStatus.BAD_REQUEST, "Payment has not been verified"),
    OAUTH2_EMAIL_CANNOT_BE_CHANGED(400, HttpStatus.BAD_REQUEST, "OAuth2 account email is managed by the login provider"),
    INVALID_AVATAR(400, HttpStatus.BAD_REQUEST, "Invalid avatar image"),
    UNSUPPORTED_AVATAR_TYPE(415, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported avatar image type"),
    AVATAR_TOO_LARGE(413, HttpStatus.PAYLOAD_TOO_LARGE, "Avatar image is too large"),
    AVATAR_STORAGE_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store avatar image"),
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
