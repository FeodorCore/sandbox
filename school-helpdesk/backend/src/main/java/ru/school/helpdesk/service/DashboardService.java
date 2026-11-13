package ru.school.helpdesk.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.domain.Priority;
import ru.school.helpdesk.domain.TicketStatus;
import ru.school.helpdesk.dto.dashboard.DashboardStats;
import ru.school.helpdesk.dto.dashboard.DashboardStats.DayCount;
import ru.school.helpdesk.dto.dashboard.DashboardStats.NameCount;
import ru.school.helpdesk.dto.dashboard.DashboardStats.Workload;
import ru.school.helpdesk.repository.TicketRepository;
import ru.school.helpdesk.security.SecurityUtils;
import ru.school.helpdesk.security.UserPrincipal;

/** Расчёт статистики для панели мониторинга. */
@Service
public class DashboardService {

    private static final List<TicketStatus> ACTIVE_STATUSES =
            List.of(TicketStatus.NEW, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);
    private static final List<TicketStatus> DONE_STATUSES =
            List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    public DashboardService(TicketRepository ticketRepository, TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats(int trendDays) {
        UserPrincipal me = SecurityUtils.principal();
        // обычный сотрудник видит статистику только по своим заявкам
        Long authorId = me.isStaff() ? null : me.getId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        int days = Math.max(1, Math.min(trendDays, 90));
        Instant trendFrom = today.minusDays(days - 1L).atStartOfDay(zone).toInstant();

        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (Object[] row : ticketRepository.countGroupByStatus(authorId)) {
            byStatus.put((TicketStatus) row[0], toLong(row[1]));
        }
        long newCount = byStatus.getOrDefault(TicketStatus.NEW, 0L);
        long inProgress = byStatus.getOrDefault(TicketStatus.IN_PROGRESS, 0L);
        long onHold = byStatus.getOrDefault(TicketStatus.ON_HOLD, 0L);
        long resolved = byStatus.getOrDefault(TicketStatus.RESOLVED, 0L);
        long closed = byStatus.getOrDefault(TicketStatus.CLOSED, 0L);
        long rejected = byStatus.getOrDefault(TicketStatus.REJECTED, 0L);
        long total = newCount + inProgress + onHold + resolved + closed + rejected;
        long active = newCount + inProgress + onHold;

        List<NameCount> statusList = Arrays.stream(TicketStatus.values())
                .map(s -> new NameCount(s.name(), s.getTitle(), byStatus.getOrDefault(s, 0L), null))
                .toList();

        List<NameCount> byPriority = new ArrayList<>();
        Map<Priority, Long> priorityMap = new EnumMap<>(Priority.class);
        for (Object[] row : ticketRepository.countGroupByPriority(authorId, ACTIVE_STATUSES)) {
            priorityMap.put((Priority) row[0], toLong(row[1]));
        }
        for (Priority p : Priority.values()) {
            byPriority.add(new NameCount(p.name(), p.getTitle(), priorityMap.getOrDefault(p, 0L), null));
        }
        long critical = priorityMap.getOrDefault(Priority.CRITICAL, 0L);

        List<NameCount> byCategory = ticketRepository.countGroupByCategory(authorId).stream()
                .map(row -> new NameCount(String.valueOf(row[0]), (String) row[1], toLong(row[3]), (String) row[2]))
                .toList();

        List<Workload> workload = me.isStaff()
                ? ticketRepository.workloadByAssignee(ACTIVE_STATUSES, DONE_STATUSES).stream()
                        .map(row -> new Workload(toLong(row[0]), (String) row[1], toLong(row[2]), toLong(row[3])))
                        .toList()
                : List.of();

        List<NameCount> topRooms = ticketRepository.topRooms(authorId, PageRequest.of(0, 6)).stream()
                .map(row -> new NameCount((String) row[0], "Кабинет " + row[0], toLong(row[1]), null))
                .toList();

        List<DayCount> trend = buildTrend(ticketRepository.createdSince(authorId, trendFrom),
                ticketRepository.resolvedSince(authorId, trendFrom), today, days, zone);

        Double avgResolutionHours = averageResolutionHours(authorId, startOfToday.minus(Duration.ofDays(30)));

        return new DashboardStats(
                !me.isStaff(),
                total,
                active,
                newCount,
                inProgress,
                onHold,
                resolved,
                closed,
                rejected,
                ticketRepository.countUnassigned(authorId, ACTIVE_STATUSES),
                ticketRepository.countOverdue(authorId, today, ACTIVE_STATUSES),
                critical,
                ticketRepository.countCreatedSince(authorId, startOfToday),
                ticketRepository.countResolvedSince(authorId, startOfToday),
                avgResolutionHours,
                statusList,
                byPriority,
                byCategory,
                workload,
                topRooms,
                trend,
                ticketService.recent(8));
    }

    private Double averageResolutionHours(Long authorId, Instant from) {
        List<Object[]> rows = ticketRepository.resolutionTimes(authorId, from);
        if (rows.isEmpty()) {
            return null;
        }
        double totalHours = 0;
        int count = 0;
        for (Object[] row : rows) {
            if (row[0] instanceof Instant created && row[1] instanceof Instant resolvedAt) {
                double hours = Duration.between(created, resolvedAt).toMinutes() / 60.0;
                if (hours >= 0) {
                    totalHours += hours;
                    count++;
                }
            }
        }
        return count == 0 ? null : Math.round(totalHours / count * 10) / 10.0;
    }

    private List<DayCount> buildTrend(List<Instant> created, List<Instant> resolved,
                                      LocalDate today, int days, ZoneId zone) {
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            buckets.put(today.minusDays(i), new long[2]);
        }
        for (Instant instant : created) {
            long[] bucket = buckets.get(instant.atZone(zone).toLocalDate());
            if (bucket != null) {
                bucket[0]++;
            }
        }
        for (Instant instant : resolved) {
            long[] bucket = buckets.get(instant.atZone(zone).toLocalDate());
            if (bucket != null) {
                bucket[1]++;
            }
        }
        List<DayCount> result = new ArrayList<>();
        buckets.forEach((date, values) -> result.add(new DayCount(date.toString(), values[0], values[1])));
        return result;
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}


