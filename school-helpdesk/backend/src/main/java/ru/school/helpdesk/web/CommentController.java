package ru.school.helpdesk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.school.helpdesk.dto.comment.CommentDto;
import ru.school.helpdesk.dto.comment.CommentRequest;
import ru.school.helpdesk.service.CommentService;

/** Комментарии к заявкам. */
@Tag(name = "Комментарии")
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Комментарии заявки (внутренние видны только специалистам)")
    @GetMapping("/tickets/{ticketId}/comments")
    public ResponseEntity<List<CommentDto>> list(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.findByTicket(ticketId));
    }

    @Operation(summary = "Добавить комментарий")
    @PostMapping("/tickets/{ticketId}/comments")
    public ResponseEntity<CommentDto> create(@PathVariable Long ticketId,
                                             @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(ticketId, request));
    }

    @Operation(summary = "Удалить комментарий (автор или ADMIN)")
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


