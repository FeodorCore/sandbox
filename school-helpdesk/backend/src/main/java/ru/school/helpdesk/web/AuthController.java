package ru.school.helpdesk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.school.helpdesk.dto.auth.ChangePasswordRequest;
import ru.school.helpdesk.dto.auth.LoginRequest;
import ru.school.helpdesk.dto.auth.LoginResponse;
import ru.school.helpdesk.dto.user.UserDto;
import ru.school.helpdesk.service.AuthService;

/** Аутентификация и текущий пользователь. */
@Tag(name = "Аутентификация")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Вход в систему: возвращает JWT-токен и данные пользователя")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Текущий пользователь")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(authService.currentUser());
    }

    @Operation(summary = "Смена собственного пароля")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Выход (токен stateless — клиент просто удаляет его)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}


