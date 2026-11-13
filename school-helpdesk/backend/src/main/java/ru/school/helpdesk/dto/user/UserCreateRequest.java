package ru.school.helpdesk.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.school.helpdesk.domain.Role;

/** Создание пользователя (только ADMIN). */
public record UserCreateRequest(
        @NotBlank(message = "Укажите логин")
        @Size(min = 3, max = 64, message = "Логин: от 3 до 64 символов")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Логин: только латиница, цифры, точка, дефис, подчёркивание")
        String username,

        @NotBlank(message = "Укажите пароль")
        @Size(min = 6, max = 72, message = "Пароль: от 6 до 72 символов")
        String password,

        @NotBlank(message = "Укажите ФИО")
        @Size(max = 160, message = "ФИО: не более 160 символов")
        String fullName,

        @Email(message = "Некорректный e-mail")
        @Size(max = 120)
        String email,

        @Size(max = 120)
        String position,

        @Size(max = 40)
        String phone,

        @NotNull(message = "Укажите роль")
        Role role,

        Boolean active) {
}


