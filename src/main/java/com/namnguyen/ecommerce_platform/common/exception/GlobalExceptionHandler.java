package com.namnguyen.ecommerce_platform.common.exception;

import com.namnguyen.ecommerce_platform.cart.exception.InvalidCartStateException;
import com.namnguyen.ecommerce_platform.cart.exception.InvalidQuantityException;
import com.namnguyen.ecommerce_platform.common.response.ValidationErrorResponse;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderException;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.payment.exception.InvalidPaymentStateException;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.namnguyen.ecommerce_platform.common.response.ApiErrorResponse;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.namnguyen.ecommerce_platform.common.exception.error.ExceptionErrorMessages.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private void logClientError(
            HttpStatus status,
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Request failed status={} method={} path={} exception={}",
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getClass().getSimpleName()
        );
    }

    private String resolveFieldErrorMessage(FieldError fieldError) {
        if ("typeMismatch".equals(fieldError.getCode())) {
            return invalidParameter(fieldError.getField());
        }

        return fieldError.getDefaultMessage();
    }

    private String resolveInvalidJsonFieldName(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException invalidFormatException
                && !invalidFormatException.getPath().isEmpty()) {
            var path = invalidFormatException.getPath();
            var fieldReference = path.get(path.size() - 1);

            if (fieldReference.getPropertyName() != null) {
                return fieldReference.getPropertyName();
            }
        }

        return "requestBody";
    }

    private String resolveInvalidJsonFieldMessage(
            HttpMessageNotReadableException ex,
            String fieldName
    ) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException invalidFormatException) {
            Class<?> targetType = invalidFormatException.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                return invalidEnumValue(
                        invalidFormatException.getValue(),
                        fieldName,
                        targetType.getEnumConstants()
                );
            }
        }

        return invalidParameter(fieldName);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ValidationErrorResponse> handleBindException(
            BindException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        log.warn(
                "Request validation failed status={} method={} path={} fieldErrorCount={}",
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getBindingResult().getFieldErrorCount()
        );

        Map<String, List<String>> fieldsErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(this::resolveFieldErrorMessage, Collectors.toList())
                ));

        return ResponseEntity.status(status)
                .body(new ValidationErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        VALIDATION_FAILED,
                        request.getRequestURI(),
                        fieldsErrors
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        INVALID_CREDENTIALS,
                        request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        log.warn(
                "Request argument validation failed status={} method={} path={} fieldErrorCount={}",
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getBindingResult().getFieldErrorCount()
        );

        Map<String, List<String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(this::resolveFieldErrorMessage,
                                Collectors.toList())
                ));

        return ResponseEntity.status(status)
                .body(new ValidationErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        VALIDATION_FAILED,
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        Map<String, List<String>> fieldErrors = ex.getParameterValidationResults()
                .stream()
                .collect(Collectors.toMap(
                        result -> result.getMethodParameter().getParameterName(),
                        result -> result.getResolvableErrors()
                                .stream()
                                .map(error -> invalidParameter(result.getMethodParameter().getParameterName()))
                                .toList(),
                        (existing, replacement) -> existing
                ));

        return ResponseEntity.status(status)
                .body(new ValidationErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        VALIDATION_FAILED,
                        request.getRequestURI(),
                        fieldErrors));
    }



    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientStockException(InsufficientStockException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderStateException(InvalidOrderStateException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(InvalidCartStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCartStateException(InvalidCartStateException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidQuantityException(InvalidQuantityException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderException(InvalidOrderException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPaymentStateException(InvalidPaymentStateException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        logClientError(status, ex, request);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        String message;

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message = invalidEnumValue(
                    ex.getValue(),
                    ex.getName(),
                    ex.getRequiredType().getEnumConstants()
            );
        } else {
            message = invalidParameter(ex.getName());
        }

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        Map<String, List<String>> fieldErrors = Map.of(
          ex.getParameterName(),
          List.of(invalidParameter(ex.getParameterName()))
        );

        return ResponseEntity.status(status)
                .body(new ValidationErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        VALIDATION_FAILED,
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logClientError(status, ex, request);

        String fieldName = resolveInvalidJsonFieldName(ex);

        Map<String, List<String>> fieldErrors = Map.of(
                fieldName,
                List.of(resolveInvalidJsonFieldMessage(ex, fieldName))
        );

        return ResponseEntity.status(status)
                .body(new ValidationErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        VALIDATION_FAILED,
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error(
                "Unexpected server error method={} path={}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        UNEXPECTED_ERROR,
                        request.getRequestURI()));
    }
}
