package ru.school.helpdesk.dto.comment;

import java.time.Instant;
import ru.school.helpdesk.dto.user.UserRef;

/** Комментарий к заявке. */
public record CommentDto(
        Long id,
        Long ticketId,
        UserRef author,
        String text,
        boolean internal,
        boolean editable,
        Instant createdAt) {
}


