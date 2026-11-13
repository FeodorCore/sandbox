package ru.school.helpdesk.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Создание/обновление категории (только ADMIN). */
public record CategoryRequest(
        @NotBlank(message = "Укажите название категории")
        @Size(max = 80, message = "Название: не более 80 символов")
        String name,

        @Size(max = 255, message = "Описание: не более 255 символов")
        String description,

        @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Цвет в формате #RRGGBB")
        String color,

        Integer sortOrder,

        Boolean active) {
}


