package ru.school.helpdesk.exception;

import org.springframework.http.HttpStatus;

/** Конфликт данных: например, дубликат логина или категории (409). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}


