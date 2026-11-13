package ru.school.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotNull;
import ru.school.helpdesk.domain.Priority;

/** Смена приоритета заявки. */
public record PriorityChangeRequest(
        @NotNull(message = "Укажите приоритет") Priority priority) {
}


