package ru.school.helpdesk.service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.domain.Role;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.dto.common.PageDto;
import ru.school.helpdesk.dto.user.UserCreateRequest;
import ru.school.helpdesk.dto.user.UserDto;
import ru.school.helpdesk.dto.user.UserRef;
import ru.school.helpdesk.dto.user.UserUpdateRequest;
import ru.school.helpdesk.exception.ApiException;
import ru.school.helpdesk.exception.ConflictException;
import ru.school.helpdesk.exception.NotFoundException;
import ru.school.helpdesk.mapper.UserMapper;
import ru.school.helpdesk.repository.CommentRepository;
import ru.school.helpdesk.repository.TicketEventRepository;
import ru.school.helpdesk.repository.TicketRepository;
import ru.school.helpdesk.repository.UserRepository;
import ru.school.helpdesk.security.SecurityUtils;

/** Управление пользователями. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketEventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       TicketRepository ticketRepository,
                       CommentRepository commentRepository,
                       TicketEventRepository eventRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageDto<UserDto> search(String search, Role role, boolean activeOnly, Pageable pageable) {
        String query = search == null ? "" : search.trim();
        List<User> users = userRepository.search(query, role, activeOnly, pageable.getSort());
        long total = userRepository.countSearch(query, role, activeOnly);

        int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), users.size());
        int to = (int) Math.min((long) from + pageable.getPageSize(), users.size());
        List<UserDto> content = users.subList(from, to).stream().map(this::toDto).toList();
        return PageDto.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        return toDto(getEntity(id));
    }

    /** Активные специалисты (ADMIN + TECHNICIAN) для списка назначения исполнителя. */
    @Transactional(readOnly = true)
    public List<UserRef> assignees() {
        return userRepository
                .findByRoleInAndActiveTrue(List.of(Role.ADMIN, Role.TECHNICIAN),
                        org.springframework.data.domain.Sort.by("fullName"))
                .stream()
                .map(userMapper::toRef)
                .toList();
    }

    /** Все активные пользователи — для фильтра «автор» (доступно специалистам). */
    @Transactional(readOnly = true)
    public List<UserRef> allActive() {
        return userRepository
                .search("", null, true, org.springframework.data.domain.Sort.by("fullName"))
                .stream()
                .map(userMapper::toRef)
                .toList();
    }

    @Transactional
    public UserDto create(UserCreateRequest request) {
        String username = request.username().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Пользователь с логином «" + username + "» уже существует");
        }
        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("E-mail «" + request.email().trim() + "» уже используется");
        }

        User user = new User(username, passwordEncoder.encode(request.password()),
                request.fullName().trim(), request.role());
        user.setEmail(normalize(request.email()));
        user.setPosition(normalize(request.position()));
        user.setPhone(normalize(request.phone()));
        user.setActive(request.active() == null || request.active());
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UserUpdateRequest request) {
        User user = getEntity(id);

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim();
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException("E-mail «" + email + "» уже используется");
            }
            user.setEmail(email);
        } else {
            user.setEmail(null);
        }

        boolean active = request.active() == null || request.active();
        boolean losesAdmin = user.getRole() == Role.ADMIN
                && (request.role() != Role.ADMIN || !active);

        if (losesAdmin) {
            guardLastAdmin(user);
        }
        if (!active && id.equals(SecurityUtils.currentUserId())) {
            throw ApiException.badRequest("Нельзя заблокировать собственную учётную запись");
        }

        user.setFullName(request.fullName().trim());
        user.setPosition(normalize(request.position()));
        user.setPhone(normalize(request.phone()));
        user.setRole(request.role());
        user.setActive(active);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getEntity(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Текущий пароль указан неверно");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Удаление пользователя.
     * Если на него есть заявки, комментарии или события истории — выполняется блокировка
     * (мягкое удаление), чтобы не разрушать историю. Иначе запись удаляется полностью.
     */
    @Transactional
    public UserDto delete(Long id) {
        User user = getEntity(id);
        if (id.equals(SecurityUtils.currentUserId())) {
            throw ApiException.badRequest("Нельзя удалить собственную учётную запись");
        }
        boolean referenced = ticketRepository.countByAuthorId(id) > 0
                || ticketRepository.countByAssigneeId(id) > 0
                || commentRepository.countByAuthorId(id) > 0
                || eventRepository.countByActorId(id) > 0;

        if (user.getRole() == Role.ADMIN) {
            guardLastAdmin(user);
        }

        if (referenced) {
            user.setActive(false);
            return toDto(userRepository.save(user));
        }
        userRepository.delete(user);
        return null;
    }

    @Transactional(readOnly = true)
    public User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Пользователь «" + username + "» не найден"));
    }

    private void guardLastAdmin(User user) {
        long admins = userRepository.countByRole(Role.ADMIN);
        long activeAdmins = userRepository
                .findByRoleInAndActiveTrue(List.of(Role.ADMIN), org.springframework.data.domain.Sort.unsorted())
                .size();
        if (activeAdmins <= 1 && admins >= 1 && user.isActive()) {
            throw ApiException.badRequest("В системе должен остаться хотя бы один активный администратор");
        }
    }

    private UserDto toDto(User user) {
        return userMapper.toDto(user,
                ticketRepository.countByAuthorId(user.getId()),
                ticketRepository.countByAssigneeId(user.getId()));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}


