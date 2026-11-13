package ru.school.helpdesk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.school.helpdesk.dto.category.CategoryDto;
import ru.school.helpdesk.dto.category.CategoryRequest;
import ru.school.helpdesk.security.SecurityUtils;
import ru.school.helpdesk.service.CategoryService;

/** Справочник категорий заявок. */
@Tag(name = "Категории")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Список категорий (все авторизованные пользователи)")
    @GetMapping
    public ResponseEntity<List<CategoryDto>> list(@RequestParam(required = false) Boolean includeInactive,
                                                  @RequestParam(defaultValue = "false") boolean withCounts) {
        boolean inactive = includeInactive != null ? includeInactive : SecurityUtils.isAdmin();
        return ResponseEntity.ok(categoryService.findAll(inactive, withCounts));
    }

    @Operation(summary = "Категория по id")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @Operation(summary = "Создать категорию (ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @Operation(summary = "Обновить категорию (ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @Operation(summary = "Удалить категорию (ADMIN). При наличии заявок категория деактивируется")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


