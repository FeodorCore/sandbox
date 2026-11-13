package ru.school.helpdesk.service;

import java.util.LinkedHashSet;
import java.util.Set;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.domain.TicketStatus;
import ru.school.helpdesk.security.UserPrincipal;

/**
 * Вычисляет действия, доступные текущему пользователю для конкретной заявки.
 * Фронтенд использует этот набор, чтобы показывать только разрешённые кнопки,
 * при этом сервер повторно проверяет права при каждом запросе.
 */
public final class TicketActions {

    public static final String VIEW = "VIEW";
    public static final String EDIT = "EDIT";
    public static final String ASSIGN = "ASSIGN";
    public static final String CHANGE_STATUS = "CHANGE_STATUS";
    public static final String CHANGE_PRIORITY = "CHANGE_PRIORITY";
    public static final String COMMENT = "COMMENT";
    public static final String INTERNAL_COMMENT = "INTERNAL_COMMENT";
    public static final String RESOLVE = "RESOLVE";
    public static final String CLOSE = "CLOSE";
    public static final String REOPEN = "REOPEN";
    public static final String CANCEL = "CANCEL";
    public static final String DELETE = "DELETE";

    private TicketActions() {
    }

    public static Set<String> forTicket(Ticket ticket, UserPrincipal me) {
        Set<String> actions = new LinkedHashSet<>();
        actions.add(VIEW);

        TicketStatus status = ticket.getStatus();
        boolean author = ticket.getAuthor() != null && ticket.getAuthor().getId().equals(me.getId());
        boolean staff = me.isStaff();
        boolean admin = me.getRole() == ru.school.helpdesk.domain.Role.ADMIN;

        if (staff) {
            if (!status.isFinal()) {
                actions.add(EDIT);
                actions.add(ASSIGN);
                actions.add(CHANGE_STATUS);
                actions.add(CHANGE_PRIORITY);
                actions.add(COMMENT);
                actions.add(INTERNAL_COMMENT);
            }
            if (status.isActive()) {
                actions.add(RESOLVE);
            }
            if (!status.isFinal()) {
                actions.add(CLOSE);
            } else {
                actions.add(REOPEN);
            }
            if (admin) {
                actions.add(DELETE);
            }
        } else if (author) {
            actions.add(COMMENT);
            if (status == TicketStatus.NEW) {
                actions.add(EDIT);
                actions.add(CANCEL);
                actions.add(CHANGE_STATUS);
            }
            if (status == TicketStatus.RESOLVED) {
                actions.add(CLOSE);
                actions.add(REOPEN);
                actions.add(CHANGE_STATUS);
            }
        }
        return actions;
    }
}


