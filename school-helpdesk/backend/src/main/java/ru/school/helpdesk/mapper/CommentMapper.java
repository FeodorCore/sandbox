package ru.school.helpdesk.mapper;

import org.springframework.stereotype.Component;
import ru.school.helpdesk.domain.Comment;
import ru.school.helpdesk.dto.comment.CommentDto;

/** Преобразование {@link Comment} в DTO. */
@Component
public class CommentMapper {

    private final UserMapper userMapper;

    public CommentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CommentDto toDto(Comment comment, boolean editable) {
        return new CommentDto(
                comment.getId(),
                comment.getTicket().getId(),
                userMapper.toRef(comment.getAuthor()),
                comment.getText(),
                comment.isInternal(),
                editable,
                comment.getCreatedAt());
    }
}


