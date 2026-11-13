package ru.school.helpdesk.exception;

import org.springframework.http.HttpStatus;

/** Базовое исключение бизнес-логики с HTTP-статусом. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException notFound(String message) {
        return new NotFoundException(message);
    }

    public static ApiException badRequest(String message) {
        return new BadRequestException(message);
    }

    public static ApiException forbidden(String message) {
        return new ForbiddenException(message);
    }

    public static ApiException conflict(String message) {
        return new ConflictException(message);
    }
}


