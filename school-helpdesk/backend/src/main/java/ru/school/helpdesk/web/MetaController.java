package ru.school.helpdesk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.school.helpdesk.service.TicketService;

/** Справочники перечислений для интерфейса. */
@Tag(name = "Справочники")
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final TicketService ticketService;

    public MetaController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Статусы, приоритеты, роли и типы событий с русскими подписями")
    @GetMapping("/enums")
    public ResponseEntity<Map<String, List<Map<String, String>>>> enums() {
        return ResponseEntity.ok(ticketService.enums());
    }

    @Operation(summary = "Краткая информация о системе")
    @GetMapping("/about")
    public ResponseEntity<Map<String, String>> about() {
        return ResponseEntity.ok(Map.of(
                "name", "School HelpDesk",
                "version", "1.0.0",
                "description", "Система управления техническими заявками школы"));
    }
}


