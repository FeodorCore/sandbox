package ru.school.helpdesk.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.school.helpdesk.domain.Role;

/** Обновление пользователя (только ADMIN). Логин не меняется. */
public record UserUpdateRequest(
        @NotBlank(message = "Укажите ФИО")
        @Size(max = 160)
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

        Boolean active,

        /** Новый пароль — если задан, заменяет текущий. */
        @Size(min = 6, max = 72, message = "Пароль: от 6 до 72 символов")
        String password) {
}


