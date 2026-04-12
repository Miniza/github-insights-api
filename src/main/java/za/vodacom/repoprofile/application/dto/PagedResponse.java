package za.vodacom.repoprofile.application.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int perPage,
        int totalItems,
        int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> allItems, int page, int perPage) {
        int total = allItems.size();
        int totalPages = (int) Math.ceil((double) total / perPage);
        int fromIndex = Math.min((page - 1) * perPage, total);
        int toIndex = Math.min(fromIndex + perPage, total);
        List<T> content = allItems.subList(fromIndex, toIndex);
        return new PagedResponse<>(content, page, perPage, total, totalPages);
    }
}
