package ru.school.helpdesk.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.school.helpdesk.domain.Role;
import ru.school.helpdesk.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleInAndActiveTrue(Collection<Role> roles, Sort sort);

    long countByRole(Role role);

    long countByActiveTrue();

    /**
     * Поиск пользователей для админ-панели.
     * Пустой {@code search} означает «без фильтра по тексту».
     */
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:activeOnly = false or u.active = true)
              and (coalesce(:search, '') = ''
                   or lower(u.username) like lower(concat('%', :search, '%'))
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.position, '')) like lower(concat('%', :search, '%')))
            """)
    List<User> search(@Param("search") String search,
                      @Param("role") Role role,
                      @Param("activeOnly") boolean activeOnly,
                      Sort sort);

    @Query("""
            select count(u) from User u
            where (:role is null or u.role = :role)
              and (:activeOnly = false or u.active = true)
              and (coalesce(:search, '') = ''
                   or lower(u.username) like lower(concat('%', :search, '%'))
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.position, '')) like lower(concat('%', :search, '%')))
            """)
    long countSearch(@Param("search") String search,
                     @Param("role") Role role,
                     @Param("activeOnly") boolean activeOnly);
}


