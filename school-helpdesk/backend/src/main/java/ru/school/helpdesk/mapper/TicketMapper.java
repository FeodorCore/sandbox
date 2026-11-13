package ru.school.helpdesk.mapper;

import java.util.Set;
import org.springframework.stereotype.Component;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.dto.ticket.TicketDto;

/** Преобразование {@link Ticket} в DTO. */
@Component
public class TicketMapper {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    public TicketMapper(UserMapper userMapper, CategoryMapper categoryMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
    }

    public TicketDto toDto(Ticket ticket, long commentCount, Set<String> actions) {
        return new TicketDto(
                ticket.getId(),
                ticket.getNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getStatus().getTitle(),
                ticket.getPriority(),
                ticket.getPriority().getTitle(),
                categoryMapper.toDto(ticket.getCategory()),
                userMapper.toRef(ticket.getAuthor()),
                userMapper.toRef(ticket.getAssignee()),
                ticket.getRoom(),
                ticket.getBuilding(),
                ticket.getEquipment(),
                ticket.getDueDate(),
                ticket.isOverdue(),
                ticket.getResolution(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                commentCount,
                actions);
    }
}


