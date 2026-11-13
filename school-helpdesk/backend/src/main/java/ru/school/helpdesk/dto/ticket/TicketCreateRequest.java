package ru.school.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import ru.school.helpdesk.domain.Priority;

/** Создание заявки. */
public record TicketCreateRequest(
        @NotBlank(message = "Укажите краткое описание проблемы")
        @Size(min = 5, max = 200, message = "Заголовок: от 5 до 200 символов")
        String title,

        @NotBlank(message = "Опишите проблему подробнее")
        @Size(min = 10, max = 4000, message = "Описание: от 10 до 4000 символов")
        String description,

        @NotNull(message = "Выберите категорию")
        Long categoryId,

        Priority priority,

        @Size(max = 40, message = "Кабинет: не более 40 символов")
        String room,

        @Size(max = 80, message = "Корпус/место: не более 80 символов")
        String building,

        @Size(max = 120, message = "Оборудование: не более 120 символов")
        String equipment,

        LocalDate dueDate,

        /** Назначить исполнителя сразу (только для ADMIN/TECHNICIAN). */
        Long assigneeId,

        /** Комментарий автора при создании. */
        @Size(max = 2000)
        String comment) {
}


