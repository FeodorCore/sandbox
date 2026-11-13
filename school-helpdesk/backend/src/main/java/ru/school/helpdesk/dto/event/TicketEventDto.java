package ru.school.helpdesk.dto.event;

import java.time.Instant;
import ru.school.helpdesk.domain.TicketEventType;
import ru.school.helpdesk.dto.user.UserRef;

/** Запись истории заявки. */
public record TicketEventDto(
        Long id,
        TicketEventType type,
        String typeTitle,
        String message,
        String fromValue,
        String toValue,
        UserRef actor,
        Instant createdAt) {
}


