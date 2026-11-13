package ru.school.helpdesk.mapper;

import org.springframework.stereotype.Component;
import ru.school.helpdesk.domain.TicketEvent;
import ru.school.helpdesk.dto.event.TicketEventDto;

/** Преобразование {@link TicketEvent} в DTO. */
@Component
public class EventMapper {

    private final UserMapper userMapper;

    public EventMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TicketEventDto toDto(TicketEvent event) {
        return new TicketEventDto(
                event.getId(),
                event.getType(),
                event.getType().getTitle(),
                event.getMessage(),
                event.getFromValue(),
                event.getToValue(),
                userMapper.toRef(event.getActor()),
                event.getCreatedAt());
    }
}


