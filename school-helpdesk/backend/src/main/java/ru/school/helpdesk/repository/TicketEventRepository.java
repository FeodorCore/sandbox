package ru.school.helpdesk.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.school.helpdesk.domain.TicketEvent;

public interface TicketEventRepository extends JpaRepository<TicketEvent, Long> {

    List<TicketEvent> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    void deleteByTicketId(Long ticketId);

    long countByActorId(Long actorId);
}


