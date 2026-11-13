package ru.school.helpdesk.repository.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import ru.school.helpdesk.domain.Category;
import ru.school.helpdesk.domain.Priority;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.domain.TicketStatus;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.dto.ticket.TicketQuery;

/**
 * Построитель динамических условий поиска заявок (поиск + фильтры).
 * Используется вместе с {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor}.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> fromQuery(TicketQuery q, Long currentUserId, boolean staff) {
        return (root, query, cb) -> {
            Map<String, Join<Ticket, ?>> joins = new HashMap<>();
            List<Predicate> predicates = new ArrayList<>();

            // --- доступ по роли: обычный сотрудник видит только свои заявки ---
            if (!staff && currentUserId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), currentUserId));
            }
            if (Boolean.TRUE.equals(q.onlyMine()) && currentUserId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), currentUserId));
            }

            // --- текстовый поиск ---
            if (q.search() != null && !q.search().isBlank()) {
                String like = "%" + q.search().trim().toLowerCase() + "%";
                Join<Ticket, User> author = join(root, joins, "author");
                Join<Ticket, User> assignee = join(root, joins, "assignee");
                Join<Ticket, Category> category = join(root, joins, "category");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("number")), like),
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("room"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("equipment"), "")), like),
                        cb.like(cb.lower(author.get("fullName")), like),
                        cb.like(cb.lower(cb.coalesce(assignee.get("fullName"), "")), like),
                        cb.like(cb.lower(category.get("name")), like)));
            }

            // --- статусы ---
            Set<TicketStatus> statuses = q.statuses();
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (Boolean.TRUE.equals(q.activeOnly())) {
                predicates.add(root.get("status").in(TicketStatus.NEW, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD));
            }

            // --- приоритеты ---
            Set<Priority> priorities = q.priorities();
            if (priorities != null && !priorities.isEmpty()) {
                predicates.add(root.get("priority").in(priorities));
            }

            // --- категория ---
            if (q.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), q.categoryId()));
            }

            // --- исполнитель ---
            if (Boolean.TRUE.equals(q.unassigned())) {
                predicates.add(root.get("assignee").isNull());
            } else if (q.assigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), q.assigneeId()));
            }

            // --- автор ---
            if (q.authorId() != null) {
                predicates.add(cb.equal(root.get("author").get("id"), q.authorId()));
            }

            // --- кабинет / корпус ---
            if (q.room() != null && !q.room().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("room")), "%" + q.room().trim().toLowerCase() + "%"));
            }
            if (q.building() != null && !q.building().isBlank()) {
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("building"), "")),
                        "%" + q.building().trim().toLowerCase() + "%"));
            }

            // --- период создания ---
            Instant from = q.dateFrom() == null ? null : q.dateFrom().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            Instant to = q.dateTo() == null ? null : q.dateTo().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }

            // --- просроченные ---
            if (Boolean.TRUE.equals(q.overdue())) {
                predicates.add(cb.isNotNull(root.get("dueDate")));
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(root.get("status").in(TicketStatus.NEW, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD));
            }

            if (query != null) {
                query.distinct(false);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Join<Ticket, T> join(Root<Ticket> root, Map<String, Join<Ticket, ?>> cache, String attribute) {
        return (Join<Ticket, T>) cache.computeIfAbsent(attribute, a -> root.join(a, JoinType.LEFT));
    }
}
