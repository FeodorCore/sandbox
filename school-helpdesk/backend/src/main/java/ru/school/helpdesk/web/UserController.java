package ru.school.helpdesk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.school.helpdesk.domain.Role;
import ru.school.helpdesk.dto.common.PageDto;
import ru.school.helpdesk.dto.user.UserCreateRequest;
import ru.school.helpdesk.dto.user.UserDto;
import ru.school.helpdesk.dto.user.UserRef;
import ru.school.helpdesk.dto.user.UserUpdateRequest;
import ru.school.helpdesk.exception.ForbiddenException;
import ru.school.helpdesk.security.SecurityUtils;
import ru.school.helpdesk.service.UserService;

/** Управление пользователями. Полный доступ — только ADMIN. */
@Tag(name = "Пользователи")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Поиск пользователей (ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageDto<UserDto>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.fromString(direction), allowedSort(sort)));
        return ResponseEntity.ok(userService.search(search, role, activeOnly, pageable));
    }

    @Operation(summary = "Список исполнителей — активные специалисты (ADMIN, TECHNICIAN)")
    @GetMapping("/assignees")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResponseEntity<List<UserRef>> assignees() {
        return ResponseEntity.ok(userService.assignees());
    }

    @Operation(summary = "Список всех активных пользователей для фильтров (ADMIN, TECHNICIAN)")
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResponseEntity<List<UserRef>> options() {
        return ResponseEntity.ok(userService.allActive());
    }

    @Operation(summary = "Пользователь по id (ADMIN или сам пользователь)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN','USER')")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin() && !id.equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("Доступно только администратору");
        }
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Создать пользователя (ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Operation(summary = "Обновить пользователя (ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Удалить или заблокировать пользователя (ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> delete(@PathVariable Long id) {
        UserDto result = userService.delete(id);
        return result == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    private static String allowedSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "username" -> "username";
            case "role" -> "role";
            case "createdAt" -> "createdAt";
            case "lastLoginAt" -> "lastLoginAt";
            case "active" -> "active";
            default -> "fullName";
        };
    }
}


