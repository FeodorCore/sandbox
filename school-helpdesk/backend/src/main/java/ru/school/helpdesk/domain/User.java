package ru.school.helpdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Пользователь системы: сотрудник школы, IT-специалист или администратор. */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = {@Index(name = "idx_users_role", columnList = "role")})
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Логин для входа в систему. */
    @Column(nullable = false, length = 64)
    private String username;

    /** Хеш пароля (BCrypt). */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Фамилия Имя Отчество. */
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(length = 120)
    private String email;

    /** Должность / предмет, например «Учитель информатики». */
    @Column(length = 120)
    private String position;

    @Column(length = 40)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    /** Признак активности: заблокированные пользователи не могут войти. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // для JPA
    }

    public User(String username, String passwordHash, String fullName, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isTechnician() {
        return role == Role.TECHNICIAN;
    }

    /** Специалист службы поддержки: администратор или IT-специалист. */
    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.TECHNICIAN;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + '}';
    }
}


