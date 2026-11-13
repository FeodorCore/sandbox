package ru.school.helpdesk.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.domain.TicketEvent;
import ru.school.helpdesk.domain.TicketEventType;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.repository.TicketEventRepository;
import ru.school.helpdesk.repository.UserRepository;
import ru.school.helpdesk.security.UserPrincipal;

/** Журналирование истории заявки (append-only). */
@Component
public class TicketHistory {

    private final TicketEventRepository eventRepository;
    private final UserRepository userRepository;

    public TicketHistory(TicketEventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(Ticket ticket, TicketEventType type, String message) {
        log(ticket, type, message, null, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(Ticket ticket, TicketEventType type, String message, String from, String to) {
        eventRepository.save(new TicketEvent(ticket, currentActor(), type, message).withValues(from, to));
    }

    private User currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return userRepository.findById(principal.getId()).orElse(null);
    }
}


