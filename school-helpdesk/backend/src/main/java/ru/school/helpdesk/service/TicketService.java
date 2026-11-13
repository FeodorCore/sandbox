package ru.school.helpdesk.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.domain.Category;
import ru.school.helpdesk.domain.Comment;
import ru.school.helpdesk.domain.Priority;
import ru.school.helpdesk.domain.Role;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.domain.TicketEventType;
import ru.school.helpdesk.domain.TicketStatus;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.dto.common.PageDto;
import ru.school.helpdesk.dto.event.TicketEventDto;
import ru.school.helpdesk.dto.ticket.AssignRequest;
import ru.school.helpdesk.dto.ticket.PriorityChangeRequest;
import ru.school.helpdesk.dto.ticket.StatusChangeRequest;
import ru.school.helpdesk.dto.ticket.TicketCreateRequest;
import ru.school.helpdesk.dto.ticket.TicketDto;
import ru.school.helpdesk.dto.ticket.TicketQuery;
import ru.school.helpdesk.dto.ticket.TicketUpdateRequest;
import ru.school.helpdesk.exception.ApiException;
import ru.school.helpdesk.exception.ForbiddenException;
import ru.school.helpdesk.exception.NotFoundException;
import ru.school.helpdesk.mapper.EventMapper;
import ru.school.helpdesk.mapper.TicketMapper;
import ru.school.helpdesk.repository.CommentRepository;
import ru.school.helpdesk.repository.TicketEventRepository;
import ru.school.helpdesk.repository.TicketRepository;
import ru.school.helpdesk.repository.UserRepository;
import ru.school.helpdesk.repository.spec.TicketSpecifications;
import ru.school.helpdesk.security.SecurityUtils;
import ru.school.helpdesk.security.UserPrincipal;

/** Основная бизнес-логика работы с заявками. */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketEventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final TicketMapper ticketMapper;
    private final EventMapper eventMapper;
    private final TicketHistory history;

    public TicketService(TicketRepository ticketRepository,
                         CommentRepository commentRepository,
                         TicketEventRepository eventRepository,
                         UserRepository userRepository,
                         CategoryService categoryService,
                         TicketMapper ticketMapper,
                         EventMapper eventMapper,
                         TicketHistory history) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.ticketMapper = ticketMapper;
        this.eventMapper = eventMapper;
        this.history = history;
    }

    // ------------------------------------------------------------------ поиск

    @Transactional(readOnly = true)
    public PageDto<TicketDto> search(TicketQuery query, Pageable pageable) {
        UserPrincipal me = SecurityUtils.principal();
        Specification<Ticket> spec = TicketSpecifications.fromQuery(query, me.getId(), me.isStaff());
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        return PageDto.of(page, toDtos(page.getContent(), me));
    }

    @Transactional(readOnly = true)
    public List<TicketDto> recent(int limit) {
        UserPrincipal me = SecurityUtils.principal();
        TicketQuery query = TicketQuery.empty();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(limit, 50)),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<Ticket> page = ticketRepository.findAll(
                TicketSpecifications.fromQuery(query, me.getId(), me.isStaff()), pageable);
        return toDtos(page.getContent(), me);
    }

    @Transactional(readOnly = true)
    public TicketDto getById(Long id) {
        UserPrincipal me = SecurityUtils.principal();
        Ticket ticket = getEntity(id);
        assertCanView(ticket, me);
        return ticketMapper.toDto(ticket, countComments(ticket.getId(), me.isStaff()),
                TicketActions.forTicket(ticket, me));
    }

    @Transactional(readOnly = true)
    public List<TicketEventDto> getHistory(Long id) {
        UserPrincipal me = SecurityUtils.principal();
        Ticket ticket = getEntity(id);
        assertCanView(ticket, me);
        return eventRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId()).stream()
                .map(eventMapper::toDto)
                .toList();
    }

    /** Заявка с проверкой права просмотра текущим пользователем. */
    @Transactional(readOnly = true)
    public Ticket getAccessible(Long id) {
        UserPrincipal me = SecurityUtils.principal();
        Ticket ticket = getEntity(id);
        assertCanView(ticket, me);
        return ticket;
    }

    @Transactional(readOnly = true)
    public Ticket getEntity(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + id + " не найдена"));
    }

    // ----------------------------------------------------------------- создание

    @Transactional
    public TicketDto create(TicketCreateRequest request) {
        UserPrincipal me = SecurityUtils.principal();
        User author = userRepository.findById(me.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Category category = categoryService.getEntity(request.categoryId());
        if (!category.isActive() && !me.isStaff()) {
            throw ApiException.badRequest("Категория «" + category.getName() + "» недоступна для выбора");
        }

        Ticket ticket = new Ticket(request.title().trim(), request.description().trim(), category, author);
        ticket.setPriority(request.priority() == null ? Priority.MEDIUM : request.priority());
        ticket.setRoom(trim(request.room()));
        ticket.setBuilding(trim(request.building()));
        ticket.setEquipment(trim(request.equipment()));
        ticket.setDueDate(request.dueDate());
        ticket.setStatus(TicketStatus.NEW);

        if (request.assigneeId() != null) {
            SecurityUtils.requireStaff();
            ticket.setAssignee(loadAssignee(request.assigneeId()));
        }

        ticket = ticketRepository.save(ticket);
        ticket.setNumber(String.format("HD-%05d", ticket.getId()));
        ticket = ticketRepository.save(ticket);

        history.log(ticket, TicketEventType.CREATED,
                "Заявка " + ticket.getNumber() + " создана: " + ticket.getTitle());

        if (request.comment() != null && !request.comment().isBlank()) {
            commentRepository.save(new Comment(ticket, author, request.comment().trim(), false));
            history.log(ticket, TicketEventType.COMMENT_ADDED, "Добавлен комментарий автора");
        }
        return getById(ticket.getId());
    }

    // -------------------------------------------------------------- редактирование

    @Transactional
    public TicketDto update(Long id, TicketUpdateRequest request) {
        UserPrincipal me = SecurityUtils.principal();
        Ticket ticket = getEntity(id);
        assertCanEdit(ticket, me);

        List<String> changes = new ArrayList<>();
        String newTitle = request.title().trim();
        if (!Objects.equals(ticket.getTitle(), newTitle)) {
            changes.add("заголовок");
            ticket.setTitle(newTitle);
        }
        String newDescription = request.description().trim();
        if (!Objects.equals(ticket.getDescription(), newDescription)) {
            changes.add("описание");
            ticket.setDescription(newDescription);
        }
        if (!Objects.equals(ticket.getRoom(), trim(request.room()))) {
            changes.add("кабинет");
            ticket.setRoom(trim(request.room()));
        }
        if (!Objects.equals(ticket.getBuilding(), trim(request.building()))) {
            changes.add("место");
            ticket.setBuilding(trim(request.building()));
        }
        if (!Objects.equals(ticket.getEquipment(), trim(request.equipment()))) {
            changes.add("оборудование");
            ticket.setEquipment(trim(request.equipment()));
        }
        if (!Objects.equals(ticket.getDueDate(), request.dueDate())) {
            changes.add("плановый срок");
            ticket.setDueDate(request.dueDate());
        }

        Category category = categoryService.getEntity(request.categoryId());
        if (!category.getId().equals(ticket.getCategory().getId())) {
            history.log(ticket, TicketEventType.CATEGORY_CHANGED,
                    "Категория изменена: " + ticket.getCategory().getName() + " → " + category.getName(),
                    ticket.getCategory().getName(), category.getName());
            ticket.setCategory(category);
            changes.add("категория");
        }

        // приоритет может менять только специалист службы поддержки
        if (me.isStaff() && request.priority() != null && request.priority() != ticket.getPriority()) {
            history.log(ticket, TicketEventType.PRIORITY_CHANGED,
                    "Приоритет изменён: " + ticket.getPriority().getTitle() + " → " + request.priority().getTitle(),
                    ticket.getPriority().name(), request.priority().name());
            ticket.setPriority(request.priority());
            changes.add("приоритет");
        }

        if (changes.isEmpty()) {
            return getById(ticket.getId());
        }
        ticketRepository.save(ticket);
        history.log(ticket, TicketEventType.UPDATED, "Изменены поля: " + String.join(", ", changes));
        return getById(ticket.getId());
    }

    // ------------------------------------------------------------------- статусы

    @Transactional
    public TicketDto changeStatus(Long id, StatusChangeRequest request) {
        UserPrincipal me = SecurityUtils.principal();
        Ticket ticket = getEntity(id);
        TicketStatus from = ticket.getStatus();
        TicketStatus to = request.status();

        if (from == to) {
            throw ApiException.badRequest("Заявка уже находится в статусе «" + to.getTitle() + "»");
        }
        assertTransitionAllowed(ticket, to, me);

        if (to == TicketStatus.RESOLVED) {
            String resolution = trim(request.resolution());
            if (resolution == null) {
                throw ApiException.badRequest("Укажите, что было сделано для решения проблемы");
            }
            ticket.setResolution(resolution);
            ticket.setResolvedAt(Instant.now());
        } else if (from == TicketStatus.RESOLVED && to != TicketStatus.CLOSED) {
            // возврат в работу — снимаем отметку о решении
            ticket.setResolvedAt(null);
            ticket.setResolution(null);
        }

        if (to == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
            if (ticket.getResolvedAt() == null && trim(request.resolution()) != null) {
                ticket.setResolution(trim(request.resolution()));
            }
        }
        if (to == TicketStatus.REJECTED) {
            ticket.setClosedAt(Instant.now());
        }

        // если специалист берёт заявку в работу и исполнитель не назначен — назначаем его
        User actor = userRepository.findById(me.getId()).orElse(null);
        if (to == TicketStatus.IN_PROGRESS && ticket.getAssignee() == null
                && me.isStaff() && actor != null && actor.isStaff()) {
            ticket.setAssignee(actor);
            history.log(ticket, TicketEventType.ASSIGNED,
                    "Исполнитель назначен автоматически: " + actor.getFullName(), null, actor.getFullName());
        }

        ticket.setStatus(to);
        ticketRepository.save(ticket);

        history.log(ticket, TicketEventType.STATUS_CHANGED,
                "Статус изменён: " + from.getTitle() + " → " + to.getTitle(), from.name(), to.name());

        if (to == TicketStatus.RESOLVED) {
            history.log(ticket, TicketEventType.RESOLVED, "Работа выполнена: " + ticket.getResolution());
        } else if (to == TicketStatus.CLOSED) {
            history.log(ticket, TicketEventType.CLOSED,
                    me.isStaff() ? "Заявка закрыта" : "Автор подтвердил выполнение заявки");
        } else if (from == TicketStatus.RESOLVED || from == TicketStatus.CLOSED || from == TicketStatus.REJECTED) {
            history.log(ticket, TicketEventType.REOPENED, "Заявка открыта повторно");
        }

        if (trim(request.comment()) != null) {
            commentRepository.save(new Comment(ticket, requireActor(), trim(request.comment()), false));
            history.log(ticket, TicketEventType.COMMENT_ADDED, "Добавлен комментарий при смене статуса");
        }
        return getById(ticket.getId());
    }

    @Transactional
    public TicketDto changePriority(Long id, PriorityChangeRequest request) {
        UserPrincipal me = SecurityUtils.principal();
        SecurityUtils.requireStaff();
        Ticket ticket = getEntity(id);
        if (ticket.getStatus().isFinal()) {
            throw ApiException.badRequest("Нельзя изменить приоритет завершённой заявки");
        }
        Priority from = ticket.getPriority();
        if (from == request.priority()) {
            return getById(id);
        }
        ticket.setPriority(request.priority());
        ticketRepository.save(ticket);
        history.log(ticket, TicketEventType.PRIORITY_CHANGED,
                "Приоритет изменён: " + from.getTitle() + " → " + request.priority().getTitle(),
                from.name(), request.priority().name());
        return getById(id);
    }

    @Transactional
    public TicketDto assign(Long id, AssignRequest request) {
        UserPrincipal me = SecurityUtils.principal();
        SecurityUtils.requireStaff();
        Ticket ticket = getEntity(id);
        if (ticket.getStatus().isFinal()) {
            throw ApiException.badRequest("Нельзя менять исполнителя у завершённой заявки");
        }

        User previous = ticket.getAssignee();
        if (request.assigneeId() == null) {
            if (previous == null) {
                throw ApiException.badRequest("У заявки нет исполнителя");
            }
            ticket.setAssignee(null);
            ticketRepository.save(ticket);
            history.log(ticket, TicketEventType.UNASSIGNED,
                    "Исполнитель снят: " + previous.getFullName(), previous.getFullName(), null);
            return getById(id);
        }

        User assignee = loadAssignee(request.assigneeId());
        if (previous != null && previous.getId().equals(assignee.getId())) {
            return getById(id);
        }
        ticket.setAssignee(assignee);
        // назначение означает, что заявка взята в работу
        if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            history.log(ticket, TicketEventType.STATUS_CHANGED,
                    "Статус изменён: Новая → В работе", TicketStatus.NEW.name(), TicketStatus.IN_PROGRESS.name());
        }
        ticketRepository.save(ticket);
        history.log(ticket, TicketEventType.ASSIGNED,
                "Назначен исполнитель: " + assignee.getFullName(),
                previous == null ? null : previous.getFullName(), assignee.getFullName());

        if (trim(request.comment()) != null) {
            commentRepository.save(new Comment(ticket, requireActor(), trim(request.comment()), false));
        }
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        SecurityUtils.requireAdmin();
        Ticket ticket = getEntity(id);
        commentRepository.deleteByTicketId(ticket.getId());
        eventRepository.deleteByTicketId(ticket.getId());
        commentRepository.flush();
        eventRepository.flush();
        ticketRepository.delete(ticket);
    }

    // ------------------------------------------------------------------- доступы

    private void assertCanView(Ticket ticket, UserPrincipal me) {
        if (me.isStaff()) {
            return;
        }
        if (!ticket.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("У вас нет доступа к этой заявке");
        }
    }

    private void assertCanEdit(Ticket ticket, UserPrincipal me) {
        if (ticket.getStatus().isFinal()) {
            throw ApiException.badRequest("Заявка завершена — редактирование недоступно");
        }
        if (me.isStaff()) {
            return;
        }
        if (!ticket.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("У вас нет доступа к этой заявке");
        }
        if (ticket.getStatus() != TicketStatus.NEW) {
            throw new ForbiddenException("Автор может редактировать заявку, только пока она новая");
        }
    }

    private void assertTransitionAllowed(Ticket ticket, TicketStatus target, UserPrincipal me) {
        if (me.isStaff()) {
            return;
        }
        if (!ticket.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("У вас нет доступа к этой заявке");
        }
        TicketStatus current = ticket.getStatus();
        boolean allowed = switch (target) {
            // автор может отменить новую заявку или подтвердить выполненную
            case CLOSED -> current == TicketStatus.NEW || current == TicketStatus.RESOLVED;
            // автор может вернуть выполненную заявку в работу, если проблема осталась
            case IN_PROGRESS -> current == TicketStatus.RESOLVED;
            default -> false;
        };
        if (!allowed) {
            throw new ForbiddenException("Переход из статуса «" + current.getTitle()
                    + "» в «" + target.getTitle() + "» недоступен автору заявки");
        }
    }

    // ------------------------------------------------------------------- служебное

    private User loadAssignee(Long assigneeId) {
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new NotFoundException("Исполнитель с id=" + assigneeId + " не найден"));
        if (!assignee.isActive()) {
            throw ApiException.badRequest("Пользователь «" + assignee.getFullName() + "» заблокирован");
        }
        Set<Role> staffRoles = new HashSet<>(List.of(Role.ADMIN, Role.TECHNICIAN));
        if (!staffRoles.contains(assignee.getRole())) {
            throw ApiException.badRequest("Исполнителем может быть только IT-специалист или администратор");
        }
        return assignee;
    }

    private User requireActor() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private List<TicketDto> toDtos(List<Ticket> tickets, UserPrincipal me) {
        if (tickets.isEmpty()) {
            return List.of();
        }
        List<Long> ids = tickets.stream().map(Ticket::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        List<Object[]> rows = me.isStaff()
                ? commentRepository.countByTicketIds(ids)
                : commentRepository.countVisibleByTicketIds(ids);
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return tickets.stream()
                .map(t -> ticketMapper.toDto(t, counts.getOrDefault(t.getId(), 0L), TicketActions.forTicket(t, me)))
                .toList();
    }

    private long countComments(Long ticketId, boolean staff) {
        return staff
                ? commentRepository.countByTicketId(ticketId)
                : commentRepository.countByTicketIdAndInternalFalse(ticketId);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Значения перечислений для справочника интерфейса. */
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, String>>> enums() {
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("statuses", java.util.Arrays.stream(TicketStatus.values())
                .map(s -> Map.of("value", s.name(), "title", s.getTitle(),
                        "active", String.valueOf(s.isActive()), "final", String.valueOf(s.isFinal())))
                .toList());
        result.put("priorities", java.util.Arrays.stream(Priority.values())
                .map(p -> Map.of("value", p.name(), "title", p.getTitle(), "weight", String.valueOf(p.getWeight())))
                .toList());
        result.put("roles", java.util.Arrays.stream(Role.values())
                .map(r -> Map.of("value", r.name(), "title", r.getTitle()))
                .toList());
        result.put("eventTypes", java.util.Arrays.stream(TicketEventType.values())
                .map(e -> Map.of("value", e.name(), "title", e.getTitle()))
                .toList());
        return Collections.unmodifiableMap(result);
    }
}


