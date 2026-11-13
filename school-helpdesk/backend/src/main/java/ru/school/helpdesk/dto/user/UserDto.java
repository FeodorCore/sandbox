package ru.school.helpdesk.dto.user;

import java.time.Instant;
import ru.school.helpdesk.domain.Role;

/** Полное представление пользователя (админ-панель, профиль). */
public record UserDto(
        Long id,
        String username,
        String fullName,
        String email,
        String position,
        String phone,
        Role role,
        String roleTitle,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt,
        Long ticketsCreated,
        Long ticketsAssigned) {
}


