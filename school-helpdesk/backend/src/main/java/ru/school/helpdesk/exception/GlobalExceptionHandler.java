package ru.school.helpdesk.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.school.helpdesk.dto.common.ApiError;

/** Единый обработчик ошибок REST API: всегда возвращает JSON в формате {@link ApiError}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getStatus().getReasonPhrase(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> violations = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", error.getField());
            item.put("message", error.getDefaultMessage());
            violations.add(item);
        }
        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", error.getObjectName());
            item.put("message", error.getDefaultMessage());
            violations.add(item);
        });
        String message = violations.isEmpty() ? "Ошибка проверки данных" : violations.get(0).get("message");
        return build(HttpStatus.BAD_REQUEST, "Ошибка проверки данных", message, request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<Map<String, String>> violations = new ArrayList<>();
        ex.getConstraintViolations().forEach(v -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", v.getPropertyPath().toString());
            item.put("message", v.getMessage());
            violations.add(item);
        });
        return build(HttpStatus.BAD_REQUEST, "Ошибка проверки данных", ex.getMessage(), request, violations);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Некорректный запрос",
                "Проверьте формат переданных данных", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Доступ запрещён", "Недостаточно прав для выполнения операции", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Требуется авторизация", "Неверный логин или пароль", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Не найдено", "Запрошенный ресурс не существует", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Необработанная ошибка на {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера",
                "Произошла непредвиденная ошибка. Обратитесь к администратору.", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message,
                                           HttpServletRequest request, List<Map<String, String>> violations) {
        ApiError body = ApiError.of(status.value(), error, message, request.getRequestURI(), violations);
        return ResponseEntity.status(status).body(body);
    }
}


