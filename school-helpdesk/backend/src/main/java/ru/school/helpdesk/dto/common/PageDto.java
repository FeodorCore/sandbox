package ru.school.helpdesk.dto.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** Универсальная страница результатов. */
public record PageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageDto<T> of(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    public static <S, T> PageDto<T> of(Page<S> page, List<T> content) {
        return new PageDto<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    public static <T> PageDto<T> of(List<T> content, int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageDto<>(content, page, size, total, totalPages,
                page <= 0, page >= totalPages - 1);
    }
}


