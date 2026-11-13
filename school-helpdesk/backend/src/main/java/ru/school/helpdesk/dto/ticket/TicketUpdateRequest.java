package ru.school.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import ru.school.helpdesk.domain.Priority;

/** Редактирование содержимого заявки (без смены статуса). */
public record TicketUpdateRequest(
        @NotBlank(message = "Укажите краткое описание проблемы")
        @Size(min = 5, max = 200, message = "Заголовок: от 5 до 200 символов")
        String title,

        @NotBlank(message = "Опишите проблему подробнее")
        @Size(min = 10, max = 4000, message = "Описание: от 10 до 4000 символов")
        String description,

        @NotNull(message = "Выберите категорию")
        Long categoryId,

        @NotNull(message = "Выберите приоритет")
        Priority priority,

        @Size(max = 40)
        String room,

        @Size(max = 80)
        String building,

        @Size(max = 120)
        String equipment,

        LocalDate dueDate) {
}


