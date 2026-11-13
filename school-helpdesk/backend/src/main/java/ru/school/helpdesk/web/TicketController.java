package ru.school.helpdesk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.school.helpdesk.domain.Priority;
import ru.school.helpdesk.domain.TicketStatus;
import ru.school.helpdesk.dto.common.PageDto;
import ru.school.helpdesk.dto.event.TicketEventDto;
import ru.school.helpdesk.dto.ticket.AssignRequest;
import ru.school.helpdesk.dto.ticket.PriorityChangeRequest;
import ru.school.helpdesk.dto.ticket.StatusChangeRequest;
import ru.school.helpdesk.dto.ticket.TicketCreateRequest;
import ru.school.helpdesk.dto.ticket.TicketDto;
import ru.school.helpdesk.dto.ticket.TicketQuery;
import ru.school.helpdesk.dto.ticket.TicketUpdateRequest;
import ru.school.helpdesk.service.TicketService;

/** Заявки: поиск, создание, редактирование, статусы, назначение, история. */
@Tag(name = "Заявки")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Поиск заявок с фильтрацией и сортировкой. "
            + "Сотрудник (USER) видит только свои заявки")
    @GetMapping
    public ResponseEntity<PageDto<TicketDto>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<TicketStatus> status,
            @RequestParam(required = false) List<Priority> priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Boolean onlyMine,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        TicketQuery query = new TicketQuery(search, toEnumSet(status, TicketStatus.class),
                toEnumSet(priority, Priority.class), categoryId, assigneeId, unassigned, authorId, onlyMine,
                room, building, dateFrom, dateTo, activeOnly, overdue);

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.fromString(direction), allowedSort(sort)));
        return ResponseEntity.ok(ticketService.search(query, pageable));
    }

    @Operation(summary = "Заявка по id")
    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @Operation(summary = "История изменений заявки")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<TicketEventDto>> history(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getHistory(id));
    }

    @Operation(summary = "Создать заявку")
    @PostMapping
    public ResponseEntity<TicketDto> create(@Valid @RequestBody TicketCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request));
    }

    @Operation(summary = "Изменить заявку (автор — пока заявка новая; специалист — до завершения)")
    @PutMapping("/{id}")
    public ResponseEntity<TicketDto> update(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketService.update(id, request));
    }

    @Operation(summary = "Изменить статус заявки")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketDto> changeStatus(@PathVariable Long id,
                                                  @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(ticketService.changeStatus(id, request));
    }

    @Operation(summary = "Изменить приоритет (ADMIN, TECHNICIAN)")
    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResponseEntity<TicketDto> changePriority(@PathVariable Long id,
                                                    @Valid @RequestBody PriorityChangeRequest request) {
        return ResponseEntity.ok(ticketService.changePriority(id, request));
    }

    @Operation(summary = "Назначить или снять исполнителя (ADMIN, TECHNICIAN)")
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResponseEntity<TicketDto> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return ResponseEntity.ok(ticketService.assign(id, request));
    }

    @Operation(summary = "Удалить заявку (ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static <E extends Enum<E>> Set<E> toEnumSet(List<E> values, Class<E> type) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(values);
    }

    /** Белый список полей сортировки — защита от произвольных выражений в ORDER BY. */
    private static String allowedSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "updatedAt" -> "updatedAt";
            case "status" -> "status";
            case "priority" -> "priority";
            case "number" -> "number";
            case "title" -> "title";
            case "room" -> "room";
            case "dueDate" -> "dueDate";
            case "resolvedAt" -> "resolvedAt";
            case "author" -> "author.fullName";
            case "assignee" -> "assignee.fullName";
            case "category" -> "category.name";
            default -> "createdAt";
        };
    }
}


