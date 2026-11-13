package ru.school.helpdesk.exception;

import org.springframework.http.HttpStatus;

/** Объект не найден (404). */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}


