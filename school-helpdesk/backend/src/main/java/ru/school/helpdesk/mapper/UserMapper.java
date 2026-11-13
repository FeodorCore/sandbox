package ru.school.helpdesk.mapper;

import org.springframework.stereotype.Component;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.dto.user.UserDto;
import ru.school.helpdesk.dto.user.UserRef;

/** Преобразование {@link User} в DTO. */
@Component
public class UserMapper {

    public UserRef toRef(User user) {
        if (user == null) {
            return null;
        }
        return new UserRef(user.getId(), user.getUsername(), user.getFullName(), user.getRole(), user.getPosition());
    }

    public UserDto toDto(User user) {
        return toDto(user, null, null);
    }

    public UserDto toDto(User user, Long ticketsCreated, Long ticketsAssigned) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPosition(),
                user.getPhone(),
                user.getRole(),
                user.getRole().getTitle(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                ticketsCreated,
                ticketsAssigned);
    }
}


