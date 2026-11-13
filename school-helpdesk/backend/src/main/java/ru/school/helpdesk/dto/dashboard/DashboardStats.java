package ru.school.helpdesk.dto.dashboard;

import java.util.List;
import ru.school.helpdesk.dto.ticket.TicketDto;

/**
 * Сводка для панели мониторинга.
 *
 * @param scopedToUser {@code true}, если статистика ограничена заявками текущего сотрудника (роль USER)
 */
public record DashboardStats(
        boolean scopedToUser,
        long total,
        long active,
        long newCount,
        long inProgress,
        long onHold,
        long resolved,
        long closed,
        long rejected,
        long unassigned,
        long overdue,
        long critical,
        long createdToday,
        long resolvedToday,
        Double avgResolutionHours,
        List<NameCount> byStatus,
        List<NameCount> byPriority,
        List<NameCount> byCategory,
        List<Workload> workload,
        List<NameCount> topRooms,
        List<DayCount> trend,
        List<TicketDto> recent) {

    /** Подписанное число: название + количество (+ цвет для диаграмм). */
    public record NameCount(String code, String name, long count, String color) {
    }

    /** Загрузка специалиста. */
    public record Workload(Long userId, String fullName, long active, long done) {
    }

    /** Точка графика динамики за день. */
    public record DayCount(String date, long created, long resolved) {
    }
}


