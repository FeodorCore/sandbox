package ru.school.helpdesk.dto.category;

/** Категория заявки. */
public record CategoryDto(
        Long id,
        String name,
        String description,
        String color,
        int sortOrder,
        boolean active,
        Long ticketCount) {
}


