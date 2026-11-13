package ru.school.helpdesk.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.domain.Comment;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.domain.TicketEventType;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.dto.comment.CommentDto;
import ru.school.helpdesk.dto.comment.CommentRequest;
import ru.school.helpdesk.exception.ApiException;
import ru.school.helpdesk.exception.ForbiddenException;
import ru.school.helpdesk.exception.NotFoundException;
import ru.school.helpdesk.mapper.CommentMapper;
import ru.school.helpdesk.repository.CommentRepository;
import ru.school.helpdesk.repository.UserRepository;
import ru.school.helpdesk.security.SecurityUtils;
import ru.school.helpdesk.security.UserPrincipal;

/** Комментарии к заявкам. */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final CommentMapper commentMapper;
    private final TicketHistory history;

    public CommentService(CommentRepository commentRepository,
                          UserRepository userRepository,
                          TicketService ticketService,
                          CommentMapper commentMapper,
                          TicketHistory history) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.commentMapper = commentMapper;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public List<CommentDto> findByTicket(Long ticketId) {
        UserPrincipal me = SecurityUtils.principal();
        Ticket ticket = ticketService.getAccessible(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId()).stream()
                .filter(comment -> me.isStaff() || !comment.isInternal())
                .map(comment -> commentMapper.toDto(comment, canDelete(comment, me)))
                .toList();
    }

    @Transactional
    public CommentDto create(Long ticketId, CommentRequest request) {
        UserPrincipal me = SecurityUtils.principal();
        Ticket ticket = ticketService.getAccessible(ticketId);

        if (ticket.getStatus().isFinal() && !me.isStaff()) {
            throw ApiException.badRequest("Заявка завершена — комментарии недоступны");
        }
        boolean internal = Boolean.TRUE.equals(request.internal());
        if (internal && !me.isStaff()) {
            throw new ForbiddenException("Внутренние комментарии доступны только специалистам");
        }

        User author = userRepository.findById(me.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Comment comment = commentRepository.save(
                new Comment(ticket, author, request.text().trim(), internal));
        history.log(ticket, TicketEventType.COMMENT_ADDED,
                internal ? "Добавлен внутренний комментарий" : "Добавлен комментарий");
        return commentMapper.toDto(comment, true);
    }

    @Transactional
    public void delete(Long commentId) {
        UserPrincipal me = SecurityUtils.principal();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
        if (!canDelete(comment, me)) {
            throw new ForbiddenException("Нельзя удалить этот комментарий");
        }
        commentRepository.delete(comment);
    }

    private boolean canDelete(Comment comment, UserPrincipal me) {
        if (me.getRole() == ru.school.helpdesk.domain.Role.ADMIN) {
            return true;
        }
        return comment.getAuthor().getId().equals(me.getId())
                && !comment.getTicket().getStatus().isFinal();
    }
}


