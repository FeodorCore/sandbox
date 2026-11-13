package ru.school.helpdesk.domain;

/** Тип события истории заявки. */
public enum TicketEventType {
    CREATED("Заявка создана"),
    UPDATED("Данные заявки изменены"),
    STATUS_CHANGED("Статус изменён"),
    PRIORITY_CHANGED("Приоритет изменён"),
    CATEGORY_CHANGED("Категория изменена"),
    ASSIGNED("Назначен исполнитель"),
    UNASSIGNED("Исполнитель снят"),
    COMMENT_ADDED("Добавлен комментарий"),
    RESOLVED("Работа выполнена"),
    REOPENED("Заявка открыта повторно"),
    CLOSED("Заявка закрыта");

    private final String title;

    TicketEventType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}


