package ru.school.helpdesk.dto.common;

/** Вариант значения перечисления для интерфейса (код + русская подпись). */
public record EnumOption(String value, String title, String description) {

    public static EnumOption of(Enum<?> e, String title) {
        return new EnumOption(e.name(), title, null);
    }
}


