package ru.school.helpdesk.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.domain.Category;
import ru.school.helpdesk.dto.category.CategoryDto;
import ru.school.helpdesk.dto.category.CategoryRequest;
import ru.school.helpdesk.exception.ApiException;
import ru.school.helpdesk.exception.ConflictException;
import ru.school.helpdesk.exception.NotFoundException;
import ru.school.helpdesk.mapper.CategoryMapper;
import ru.school.helpdesk.repository.CategoryRepository;
import ru.school.helpdesk.repository.TicketRepository;

/** Управление справочником категорий. */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository,
                           TicketRepository ticketRepository,
                           CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
        this.categoryMapper = categoryMapper;
    }

    /**
     * Список категорий.
     *
     * @param includeInactive показывать неактивные (нужно администратору и для старых заявок)
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> findAll(boolean includeInactive, boolean withCounts) {
        List<Category> categories = includeInactive
                ? categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                : categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
        return categories.stream()
                .map(c -> categoryMapper.toDto(c, withCounts ? ticketRepository.countByCategoryId(c.getId()) : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(Long id) {
        return categoryMapper.toDto(getEntity(id), ticketRepository.countByCategoryId(id));
    }

    @Transactional(readOnly = true)
    public Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена"));
    }

    @Transactional
    public CategoryDto create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Категория «" + name + "» уже существует");
        }
        Category category = new Category(name, request.description(), normalizeColor(request.color()),
                request.sortOrder() == null ? 0 : request.sortOrder());
        category.setActive(request.active() == null || request.active());
        return categoryMapper.toDto(categoryRepository.save(category), 0L);
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Категория «" + name + "» уже существует");
        }
        boolean deactivating = category.isActive() && Boolean.FALSE.equals(request.active());
        if (deactivating && ticketRepository.countByCategoryId(id) == 0
                && categoryRepository.countByActiveTrue() <= 1) {
            throw ApiException.badRequest("В системе должна остаться хотя бы одна активная категория");
        }
        category.setName(name);
        category.setDescription(request.description());
        category.setColor(normalizeColor(request.color()));
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return categoryMapper.toDto(categoryRepository.save(category), ticketRepository.countByCategoryId(id));
    }

    /**
     * Удаление категории. Если к ней привязаны заявки — категория деактивируется
     * (мягкое удаление), история заявок сохраняется.
     */
    @Transactional
    public void delete(Long id) {
        Category category = getEntity(id);
        long used = ticketRepository.countByCategoryId(id);
        if (used > 0) {
            category.setActive(false);
            categoryRepository.save(category);
            return;
        }
        if (categoryRepository.countByActiveTrue() <= 1 && category.isActive()) {
            throw ApiException.badRequest("В системе должна остаться хотя бы одна активная категория");
        }
        categoryRepository.delete(category);
    }

    private static String normalizeColor(String color) {
        return color == null || color.isBlank() ? null : color.trim();
    }
}


