package ru.school.helpdesk.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.school.helpdesk.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    long countByTicketId(Long ticketId);

    long countByAuthorId(Long authorId);

    long countByTicketIdAndInternalFalse(Long ticketId);

    void deleteByTicketId(Long ticketId);

    /** Количество комментариев по списку заявок: [ticketId, count]. */
    @Query("select c.ticket.id, count(c) from Comment c where c.ticket.id in :ids group by c.ticket.id")
    List<Object[]> countByTicketIds(@Param("ids") Collection<Long> ids);

    /** То же, но без внутренних комментариев — для роли USER. */
    @Query("""
            select c.ticket.id, count(c) from Comment c
            where c.ticket.id in :ids and c.internal = false
            group by c.ticket.id
            """)
    List<Object[]> countVisibleByTicketIds(@Param("ids") Collection<Long> ids);
}


