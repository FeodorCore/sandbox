package ru.school.helpdesk.service;

import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.dto.auth.ChangePasswordRequest;
import ru.school.helpdesk.dto.auth.LoginRequest;
import ru.school.helpdesk.dto.auth.LoginResponse;
import ru.school.helpdesk.dto.user.UserDto;
import ru.school.helpdesk.exception.UnauthorizedException;
import ru.school.helpdesk.mapper.UserMapper;
import ru.school.helpdesk.repository.UserRepository;
import ru.school.helpdesk.security.JwtService;
import ru.school.helpdesk.security.SecurityUtils;

/** Аутентификация и работа с текущим пользователем. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new UnauthorizedException("Неверный логин или пароль"));

        // одинаковое сообщение для «нет пользователя» и «неверный пароль» — не раскрываем логины
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Неверный логин или пароль");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Учётная запись заблокирована. Обратитесь к администратору.");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return LoginResponse.of(token, jwtService.expiresInSeconds(), userMapper.toDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto currentUser() {
        return userMapper.toDto(userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден")));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Текущий пароль указан неверно");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}


