package ru.school.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.school.helpdesk.domain.TicketStatus;

/** Смена статуса заявки. */
public record StatusChangeRequest(
        @NotNull(message = "Укажите новый статус") TicketStatus status,

        /** Текст решения — обязателен при переводе в RESOLVED (проверяется в сервисе). */
        @Size(max = 2000)
        String resolution,

        /** Необязательный комментарий, который попадёт в историю. */
        @Size(max = 2000)
        String comment) {
}


