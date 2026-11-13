package ru.school.helpdesk.dto.ticket;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import ru.school.helpdesk.domain.Priority;
import ru.school.helpdesk.domain.TicketStatus;
import ru.school.helpdesk.dto.category.CategoryDto;
import ru.school.helpdesk.dto.user.UserRef;

/**
 * Полное представление заявки.
 *
 * @param actions действия, доступные текущему пользователю:
 *                EDIT, ASSIGN, CHANGE_STATUS, CHANGE_PRIORITY, COMMENT, INTERNAL_COMMENT, CLOSE, REOPEN, DELETE
 */
public record TicketDto(
        Long id,
        String number,
        String title,
        String description,
        TicketStatus status,
        String statusTitle,
        Priority priority,
        String priorityTitle,
        CategoryDto category,
        UserRef author,
        UserRef assignee,
        String room,
        String building,
        String equipment,
        LocalDate dueDate,
        boolean overdue,
        String resolution,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        Instant closedAt,
        long commentCount,
        Set<String> actions) {
}


