package ru.school.helpdesk.dto.ticket;

import jakarta.validation.constraints.Size;

/**
 * Назначение исполнителя.
 * {@code assigneeId = null} — снять назначение (заявка возвращается в очередь).
 */
public record AssignRequest(
        Long assigneeId,

        @Size(max = 2000)
        String comment) {
}


