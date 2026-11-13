package ru.school.helpdesk.domain;

/** Приоритет заявки. Число используется для сортировки (чем больше, тем важнее). */
public enum Priority {
    LOW("Низкий", 1),
    MEDIUM("Средний", 2),
    HIGH("Высокий", 3),
    CRITICAL("Критический", 4);

    private final String title;
    private final int weight;

    Priority(String title, int weight) {
        this.title = title;
        this.weight = weight;
    }

    public String getTitle() {
        return title;
    }

    public int getWeight() {
        return weight;
    }
}


