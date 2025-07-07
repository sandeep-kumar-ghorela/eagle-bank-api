package com.eaglebank.exception.exceptionhandler;

import com.eaglebank.exception.EntityNotFountException;
import com.eaglebank.exception.InSufficientBalanceException;
import com.eaglebank.exception.UserNotFoundException;
import com.eaglebank.model.ErrorDetail;
import com.eaglebank.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

import static com.eaglebank.constants.EagleBankApiConstants.ERROR_DETAIL;
import static com.eaglebank.constants.EagleBankApiConstants.INVALID_DETAILS_SUPPLIED;
import static com.eaglebank.constants.EagleBankApiConstants.MESSAGE;
import static com.eaglebank.constants.EagleBankApiConstants.VALIDATION_ERROR;

/**
 * Global exception handler for the Eagle Bank API.
 *
 * This class catches various application-wide exceptions and returns
 * appropriate HTTP responses with custom error messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors when request body fields fail validation constraints.
     * Triggered when controller method arguments are annotated with @Valid.
     *
     * @param ex the exception thrown for invalid method arguments
     * @return a BAD_REQUEST response with details about the field error
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(MESSAGE, INVALID_DETAILS_SUPPLIED);
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {

            ErrorDetail errorDetail = ErrorDetail.builder()
                    .field(error.getField())
                    .type(error.getCode())
                    .message(error.getDefaultMessage())
                    .build();
            body.put(ERROR_DETAIL, errorDetail);
        }
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation errors for path or query parameters.
     * Triggered when method parameters are annotated with @Validated.
     *
     * @param ex the exception thrown for constraint violations
     * @return a BAD_REQUEST response with validation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(MESSAGE, INVALID_DETAILS_SUPPLIED);
        ErrorDetail errorDetail = ErrorDetail.builder()
                .field(ex.getMessage())
                .type(VALIDATION_ERROR)
                .message(ex.getMessage())
                .build();
        body.put(ERROR_DETAIL, errorDetail);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles deserialization errors such as enum conversion failures or malformed JSON.
     *
     * @param ex the exception thrown during JSON parsing
     * @return a BAD_REQUEST response with error details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleEnumConversionException(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(MESSAGE, INVALID_DETAILS_SUPPLIED);
        ErrorDetail errorDetail = ErrorDetail.builder()
                .field(ex.getMessage())
                .type(VALIDATION_ERROR)
                .message(ex.getMessage())
                .build();
        body.put(ERROR_DETAIL, errorDetail);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles not-found exceptions such as missing user or bank account.
     *
     * @param ex the exception thrown when a resource is not found
     * @return a NOT_FOUND response with the error message
     */
    @ExceptionHandler({UserNotFoundException.class, EntityNotFountException.class})
    public ResponseEntity<?> handleUserAndBankAccountNotFound(RuntimeException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles insufficient balance or unsupported operations.
     *
     * @param ex the exception thrown for business logic errors
     * @return an UNPROCESSABLE_ENTITY response with the error message
     */
    @ExceptionHandler({InSufficientBalanceException.class, UnsupportedOperationException.class})
    public ResponseEntity<?> handleUnsupportedAndInSufficientBalance(RuntimeException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }


    /**
     * Handles access denied errors when user is authenticated but not authorized.
     *
     * @param ex the exception thrown for forbidden access
     * @return a FORBIDDEN response with the error message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles bad credentials.
     *
     * @param ex the exception thrown for unauthorised access
     * @return a FORBIDDEN response with the error message
     */
    @ExceptionHandler({BadCredentialsException.class, InternalAuthenticationServiceException.class})
    public ResponseEntity<?> handleBadCredentials(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles internal server error.
     *
     * @param ex the exception thrown when 500 happens
     * @return a internal server error response with the error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleInternalServerError(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}