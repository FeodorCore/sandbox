package ru.school.helpdesk.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.domain.TicketStatus;

/**
 * Доступ к заявкам.
 *
 * <p>Все агрегирующие запросы принимают необязательный {@code authorId}:
 * если он {@code null} — статистика считается по всем заявкам (ADMIN/TECHNICIAN),
 * иначе — только по заявкам конкретного сотрудника (USER).
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    long countByAuthorId(Long authorId);

    long countByAssigneeId(Long assigneeId);

    long countByCategoryId(Long categoryId);

    @Query("select count(t) from Ticket t where (:authorId is null or t.author.id = :authorId)")
    long countScoped(@Param("authorId") Long authorId);

    @Query("""
            select count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId) and t.status in :statuses
            """)
    long countByStatuses(@Param("authorId") Long authorId, @Param("statuses") Collection<TicketStatus> statuses);

    @Query("""
            select count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId)
              and t.assignee is null and t.status in :statuses
            """)
    long countUnassigned(@Param("authorId") Long authorId, @Param("statuses") Collection<TicketStatus> statuses);

    @Query("""
            select count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId)
              and t.dueDate is not null and t.dueDate < :date and t.status in :statuses
            """)
    long countOverdue(@Param("authorId") Long authorId,
                      @Param("date") LocalDate date,
                      @Param("statuses") Collection<TicketStatus> statuses);

    @Query("""
            select count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId)
              and t.createdAt >= :from
            """)
    long countCreatedSince(@Param("authorId") Long authorId, @Param("from") Instant from);

    @Query("""
            select count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId)
              and t.resolvedAt is not null and t.resolvedAt >= :from
            """)
    long countResolvedSince(@Param("authorId") Long authorId, @Param("from") Instant from);

    /** Распределение по статусам: [status, count]. */
    @Query("""
            select t.status, count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId)
            group by t.status
            """)
    List<Object[]> countGroupByStatus(@Param("authorId") Long authorId);

    /** Распределение активных заявок по приоритетам: [priority, count]. */
    @Query("""
            select t.priority, count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId) and t.status in :statuses
            group by t.priority
            order by t.priority desc
            """)
    List<Object[]> countGroupByPriority(@Param("authorId") Long authorId,
                                        @Param("statuses") Collection<TicketStatus> statuses);

    /** Статистика по категориям: [categoryId, categoryName, color, count]. */
    @Query("""
            select t.category.id, t.category.name, t.category.color, count(t)
            from Ticket t
            where (:authorId is null or t.author.id = :authorId)
            group by t.category.id, t.category.name, t.category.color
            order by count(t) desc
            """)
    List<Object[]> countGroupByCategory(@Param("authorId") Long authorId);

    /** Загрузка специалистов: [assigneeId, fullName, activeCount, doneCount]. */
    @Query("""
            select t.assignee.id, t.assignee.fullName,
                   sum(case when t.status in :activeStatuses then 1 else 0 end),
                   sum(case when t.status in :doneStatuses then 1 else 0 end)
            from Ticket t
            where t.assignee is not null
            group by t.assignee.id, t.assignee.fullName
            order by sum(case when t.status in :activeStatuses then 1 else 0 end) desc
            """)
    List<Object[]> workloadByAssignee(@Param("activeStatuses") Collection<TicketStatus> activeStatuses,
                                      @Param("doneStatuses") Collection<TicketStatus> doneStatuses);

    /** Кабинеты-«лидеры» по заявкам: [room, count]. */
    @Query("""
            select t.room, count(t) from Ticket t
            where (:authorId is null or t.author.id = :authorId)
              and t.room is not null and t.room <> ''
            group by t.room
            order by count(t) desc
            """)
    List<Object[]> topRooms(@Param("authorId") Long authorId, Pageable pageable);

    /** Метки времени создания заявок за период — для графика динамики. */
    @Query("""
            select t.createdAt from Ticket t
            where (:authorId is null or t.author.id = :authorId) and t.createdAt >= :from
            """)
    List<Instant> createdSince(@Param("authorId") Long authorId, @Param("from") Instant from);

    /** Метки времени выполнения заявок за период — для графика динамики. */
    @Query("""
            select t.resolvedAt from Ticket t
            where (:authorId is null or t.author.id = :authorId)
              and t.resolvedAt is not null and t.resolvedAt >= :from
            """)
    List<Instant> resolvedSince(@Param("authorId") Long authorId, @Param("from") Instant from);

    /** Пары [createdAt, resolvedAt] для расчёта среднего времени решения. */
    @Query("""
            select t.createdAt, t.resolvedAt from Ticket t
            where (:authorId is null or t.author.id = :authorId)
              and t.resolvedAt is not null and t.createdAt >= :from
            """)
    List<Object[]> resolutionTimes(@Param("authorId") Long authorId, @Param("from") Instant from);
}


