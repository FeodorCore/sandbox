package ru.school.helpdesk.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.school.helpdesk.domain.Role;
import ru.school.helpdesk.domain.User;

/** Принципал аутентифицированного пользователя. */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Role role;
    private final boolean active;

    public UserPrincipal(Long id, String username, String password, String fullName, Role role, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getFullName(), user.getRole(), user.isActive());
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.TECHNICIAN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}


