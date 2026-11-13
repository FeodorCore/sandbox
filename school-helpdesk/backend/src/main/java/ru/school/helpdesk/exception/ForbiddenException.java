package ru.school.helpdesk.exception;

import org.springframework.http.HttpStatus;

/** Недостаточно прав (403). */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}


