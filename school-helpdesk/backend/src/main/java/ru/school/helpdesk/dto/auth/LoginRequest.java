package ru.school.helpdesk.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Запрос на вход. */
public record LoginRequest(
        @NotBlank(message = "Укажите логин") String username,
        @NotBlank(message = "Укажите пароль") String password) {
}


