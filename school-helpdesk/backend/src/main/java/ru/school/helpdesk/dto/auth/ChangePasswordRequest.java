package ru.school.helpdesk.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Смена собственного пароля. */
public record ChangePasswordRequest(
        @NotBlank(message = "Укажите текущий пароль") String currentPassword,

        @NotBlank(message = "Укажите новый пароль")
        @Size(min = 6, max = 72, message = "Новый пароль: от 6 до 72 символов")
        String newPassword) {
}


