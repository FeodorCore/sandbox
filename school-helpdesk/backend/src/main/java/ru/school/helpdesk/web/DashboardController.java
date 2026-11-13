package ru.school.helpdesk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.school.helpdesk.dto.dashboard.DashboardStats;
import ru.school.helpdesk.service.DashboardService;

/** Панель мониторинга. */
@Tag(name = "Мониторинг")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Статистика: статусы, приоритеты, категории, загрузка специалистов, динамика")
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> stats(@RequestParam(defaultValue = "14") int trendDays) {
        return ResponseEntity.ok(dashboardService.stats(trendDays));
    }
}


