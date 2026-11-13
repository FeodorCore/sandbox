package ru.school.helpdesk.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.school.helpdesk.domain.Role;
import ru.school.helpdesk.exception.ForbiddenException;
import ru.school.helpdesk.exception.UnauthorizedException;

/** Удобный доступ к текущему пользователю из сервисного слоя. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Требуется авторизация");
        }
        return principal;
    }

    public static Long currentUserId() {
        return principal().getId();
    }

    public static Role currentRole() {
        return principal().getRole();
    }

    public static boolean isStaff() {
        Role role = currentRole();
        return role == Role.ADMIN || role == Role.TECHNICIAN;
    }

    public static boolean isAdmin() {
        return currentRole() == Role.ADMIN;
    }

    public static void requireStaff() {
        if (!isStaff()) {
            throw new ForbiddenException("Действие доступно только IT-специалисту или администратору");
        }
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("Действие доступно только администратору");
        }
    }
}


