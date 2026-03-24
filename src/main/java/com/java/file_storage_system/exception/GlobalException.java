package com.java.file_storage_system.exception;

import com.java.file_storage_system.payload.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@Slf4j
@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<String>> handleBusinessException(BaseException ex, HttpServletRequest request) {
        log.warn("Handle Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Invalid request payload");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, request.getRequestURI()));
    }

        @ExceptionHandler({
                        MissingRequestHeaderException.class,
                        MissingServletRequestParameterException.class,
                        HttpMessageNotReadableException.class
        })
        public ResponseEntity<ApiResponse<String>> handleBadRequestException(Exception ex, HttpServletRequest request) {
                String message;

                if (ex instanceof MissingRequestHeaderException missingHeaderEx) {
                        message = "Missing required header: " + missingHeaderEx.getHeaderName();
                } else if (ex instanceof MissingServletRequestParameterException missingParamEx) {
                        message = "Missing required parameter: " + missingParamEx.getParameterName();
                } else {
                        message = "Malformed JSON request body";
                }

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(message, request.getRequestURI()));
        }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleSystemException(Exception ex, HttpServletRequest request) {
        log.error("handle System exception: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred",
                        request.getRequestURI()
                ));
    }

}
