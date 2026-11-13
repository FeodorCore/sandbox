package ru.school.helpdesk.dto.auth;

import ru.school.helpdesk.dto.user.UserDto;

/** Ответ на успешный вход: JWT-токен и данные пользователя. */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserDto user) {

    public static LoginResponse of(String token, long expiresInSeconds, UserDto user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}


