package ru.school.helpdesk.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Новый комментарий. */
public record CommentRequest(
        @NotBlank(message = "Текст комментария не может быть пустым")
        @Size(min = 1, max = 2000, message = "Комментарий: не более 2000 символов")
        String text,

        /** Внутренний комментарий — виден только ADMIN и TECHNICIAN. */
        Boolean internal) {
}


