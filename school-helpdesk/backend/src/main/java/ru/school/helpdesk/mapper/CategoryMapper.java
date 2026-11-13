package ru.school.helpdesk.mapper;

import org.springframework.stereotype.Component;
import ru.school.helpdesk.domain.Category;
import ru.school.helpdesk.dto.category.CategoryDto;

/** Преобразование {@link Category} в DTO. */
@Component
public class CategoryMapper {

    public CategoryDto toDto(Category category) {
        return toDto(category, null);
    }

    public CategoryDto toDto(Category category, Long ticketCount) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getColor(),
                category.getSortOrder(),
                category.isActive(),
                ticketCount);
    }
}


