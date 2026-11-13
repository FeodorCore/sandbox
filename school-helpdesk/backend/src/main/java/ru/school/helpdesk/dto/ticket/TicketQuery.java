package ru.school.helpdesk.dto.ticket;

import java.time.LocalDate;
import java.util.Set;
import ru.school.helpdesk.domain.Priority;
import ru.school.helpdesk.domain.TicketStatus;

/**
 * Параметры фильтрации списка заявок.
 *
 * @param search     текст: номер, заголовок, описание, кабинет, ФИО, категория
 * @param statuses   набор статусов
 * @param priorities набор приоритетов
 * @param categoryId категория
 * @param assigneeId исполнитель
 * @param unassigned только неназначенные
 * @param authorId   автор
 * @param onlyMine   только мои заявки (для специалиста — где он автор)
 * @param room       кабинет (поиск по подстроке)
 * @param building   корпус
 * @param dateFrom   дата создания «с»
 * @param dateTo     дата создания «по»
 * @param activeOnly только активные (NEW / IN_PROGRESS / ON_HOLD)
 * @param overdue    только просроченные
 */
public record TicketQuery(
        String search,
        Set<TicketStatus> statuses,
        Set<Priority> priorities,
        Long categoryId,
        Long assigneeId,
        Boolean unassigned,
        Long authorId,
        Boolean onlyMine,
        String room,
        String building,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean activeOnly,
        Boolean overdue) {

    public static TicketQuery empty() {
        return new TicketQuery(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}


