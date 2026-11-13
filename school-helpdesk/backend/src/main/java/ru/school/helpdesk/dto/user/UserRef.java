package ru.school.helpdesk.dto.user;

import ru.school.helpdesk.domain.Role;

/** Краткая ссылка на пользователя (автор, исполнитель). */
public record UserRef(Long id, String username, String fullName, Role role, String position) {
}


