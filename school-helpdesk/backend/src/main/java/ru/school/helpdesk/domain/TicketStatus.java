package ru.school.helpdesk.domain;

/** Жизненный цикл заявки. */
public enum TicketStatus {
    /** Новая заявка, ещё не взята в работу. */
    NEW("Новая"),
    /** В работе у исполнителя. */
    IN_PROGRESS("В работе"),
    /** Работа приостановлена (ожидание запчастей, ответа и т. п.). */
    ON_HOLD("Отложена"),
    /** Работа выполнена, ожидается подтверждение автора. */
    RESOLVED("Выполнена"),
    /** Заявка закрыта (подтверждена автором или администратором). */
    CLOSED("Закрыта"),
    /** Отклонена: не является технической проблемой / дубликат. */
    REJECTED("Отклонена");

    private final String title;

    TicketStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Заявка считается активной (в работе службы поддержки). */
    public boolean isActive() {
        return this == NEW || this == IN_PROGRESS || this == ON_HOLD;
    }

    /** Заявка в финальном состоянии — изменения не допускаются. */
    public boolean isFinal() {
        return this == CLOSED || this == REJECTED;
    }
}


