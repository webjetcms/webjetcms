package sk.iway.iwcm.headless.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.headless.dto.ErrorResponse;
import sk.iway.iwcm.headless.dto.FieldError;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for headless API endpoints.
 * Provides consistent error responses across all headless endpoints.
 */
@RestControllerAdvice("sk.iway.iwcm.headless.rest")
public class HeadlessExceptionHandler {

    /**
     * Handles general exceptions in headless endpoints.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        Logger.error(HeadlessExceptionHandler.class, "HeadlessExceptionHandler", ex);

        String path = request.getRequestURI();
        String message = "Internal server error.";

        // Provide more specific messages for known exceptions
        if (ex.getCause() != null) {
            message = ex.getCause().getMessage();
        } else if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            message = ex.getMessage();
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                message
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles JSON processing errors.
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessingException(JsonProcessingException ex,
                                                                       HttpServletRequest request) {
        Logger.error(HeadlessExceptionHandler.class, "HeadlessExceptionHandler.handleJsonProcessingException", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Invalid JSON payload: " + ex.getOriginalMessage()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation errors with field-level details.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("request", ex.getMessage()));

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );
        errorResponse.setFieldErrors(fieldErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
