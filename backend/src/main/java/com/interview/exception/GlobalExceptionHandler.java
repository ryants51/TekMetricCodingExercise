package com.interview.exception;

import com.interview.dto.ErrorResponse;
import com.interview.dto.FieldErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException exception) {
        log.warn("Invalid request: {}", exception.getMessage());
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
        log.warn("Request conflict: {}", exception.getMessage());
        return build(HttpStatus.CONFLICT, exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();

        log.warn("Request validation failed: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations().stream()
            .map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();

        log.warn("Request constraint validation failed: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", fieldErrors);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        ConversionFailedException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        log.warn("Malformed request", exception);
        return build(
            HttpStatus.BAD_REQUEST,
            "Request contains an invalid value. Check UUIDs, enum values, and JSON types.",
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected application error", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", List.of());
    }

    private ResponseEntity<ErrorResponse> build(
        HttpStatus status,
        String message,
        List<FieldErrorResponse> fieldErrors
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            fieldErrors
        ));
    }
}
