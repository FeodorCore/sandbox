package ru.school.helpdesk.exception;

import org.springframework.http.HttpStatus;

/** Не аутентифицирован (401). */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}


