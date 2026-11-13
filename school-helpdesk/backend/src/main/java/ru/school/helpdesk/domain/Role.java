package ru.school.helpdesk.domain;

/**
 * Роли пользователей системы.
 *
 * <ul>
 *   <li>{@link #ADMIN} — полный доступ: пользователи, категории, все заявки;</li>
 *   <li>{@link #TECHNICIAN} — IT-специалист: все заявки, назначение исполнителей, смена статусов;</li>
 *   <li>{@link #USER} — сотрудник школы: создаёт заявки и видит только свои.</li>
 * </ul>
 */
public enum Role {
    ADMIN("Администратор"),
    TECHNICIAN("IT-специалист"),
    USER("Сотрудник");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Имя роли в терминах Spring Security. */
    public String authority() {
        return "ROLE_" + name();
    }
}


