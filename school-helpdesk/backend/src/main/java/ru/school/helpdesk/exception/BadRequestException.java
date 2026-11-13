package ru.school.helpdesk.exception;

import org.springframework.http.HttpStatus;

/** Некорректные данные запроса (400). */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}


