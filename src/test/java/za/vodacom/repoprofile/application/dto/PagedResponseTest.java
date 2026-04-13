package za.vodacom.repoprofile.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PagedResponse Unit Tests")
class PagedResponseTest {

    @Test
    @DisplayName("First page")
    void testPagedResponseFirstPage() {
        List<String> allItems = List.of("item1", "item2", "item3", "item4", "item5");
        int page = 1;
        int perPage = 2;

        PagedResponse<String> response = PagedResponse.of(allItems, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::page, PagedResponse::perPage, PagedResponse::totalItems, PagedResponse::totalPages)
                .containsExactly(1, 2, 5, 3);
        assertThat(response.content()).containsExactly("item1", "item2");
    }

    @Test
    @DisplayName("Middle page")
    void testPagedResponseMiddlePage() {
        List<String> allItems = List.of("item1", "item2", "item3", "item4", "item5");
        int page = 2;
        int perPage = 2;

        PagedResponse<String> response = PagedResponse.of(allItems, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::page, PagedResponse::perPage, PagedResponse::totalItems, PagedResponse::totalPages)
                .containsExactly(2, 2, 5, 3);
        assertThat(response.content()).containsExactly("item3", "item4");
    }

    @Test
    @DisplayName("Partial last page")
    void testPagedResponseLastPage() {
        List<String> allItems = List.of("item1", "item2", "item3", "item4", "item5");
        int page = 3;
        int perPage = 2;

        PagedResponse<String> response = PagedResponse.of(allItems, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::page, PagedResponse::perPage, PagedResponse::totalItems, PagedResponse::totalPages)
                .containsExactly(3, 2, 5, 3);
        assertThat(response.content()).containsExactly("item5");
    }

    @Test
    @DisplayName("Empty list")
    void testPagedResponseEmptyList() {
        List<String> allItems = List.of();
        int page = 1;
        int perPage = 10;

        PagedResponse<String> response = PagedResponse.of(allItems, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::page, PagedResponse::perPage, PagedResponse::totalItems, PagedResponse::totalPages)
                .containsExactly(1, 10, 0, 0);
        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("Single page")
    void testPagedResponseSinglePage() {
        List<String> allItems = List.of("item1", "item2", "item3");
        int page = 1;
        int perPage = 10;

        PagedResponse<String> response = PagedResponse.of(allItems, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::page, PagedResponse::perPage, PagedResponse::totalItems, PagedResponse::totalPages)
                .containsExactly(1, 10, 3, 1);
        assertThat(response.content()).containsExactlyElementsOf(allItems);
    }

    @Test
    @DisplayName("Out-of-bounds page returns empty content")
    void testPagedResponseOutOfBoundsPage() {
        List<String> allItems = List.of("item1", "item2", "item3");
        int page = 5;
        int perPage = 2;

        PagedResponse<String> response = PagedResponse.of(allItems, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::page, PagedResponse::totalPages)
                .containsExactly(5, 2);
        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("totalPages calculation")
    void testPagedResponseTotalPagesCalculation() {
        // 10 items, 3 per page = 4 pages (3, 3, 3, 1)
        PagedResponse<String> response1 = PagedResponse.of(createList(10), 1, 3);
        assertThat(response1.totalPages()).isEqualTo(4);

        // 10 items, 5 per page = 2 pages (5, 5)
        PagedResponse<String> response2 = PagedResponse.of(createList(10), 1, 5);
        assertThat(response2.totalPages()).isEqualTo(2);

        // 10 items, 1 per page = 10 pages
        PagedResponse<String> response3 = PagedResponse.of(createList(10), 1, 1);
        assertThat(response3.totalPages()).isEqualTo(10);

        // 11 items, 3 per page = 4 pages (3, 3, 3, 2)
        PagedResponse<String> response4 = PagedResponse.of(createList(11), 1, 3);
        assertThat(response4.totalPages()).isEqualTo(4);
    }

    @Test
    @DisplayName("Large dataset (1000 items)")
    void testPagedResponseLargeDataset() {
        List<String> allItems = createList(1000);
        int page = 50;
        int perPage = 10;

        PagedResponse<String> response = PagedResponse.of(allItems, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::totalItems, PagedResponse::totalPages)
                .containsExactly(1000, 100);
        assertThat(response.content()).hasSize(10);
    }

    @Test
    @DisplayName("Works with Integer and Long types")
    void testPagedResponseDifferentTypes() {
        List<Integer> intItems = List.of(1, 2, 3, 4, 5);
        List<Long> longItems = List.of(100L, 200L, 300L);

        PagedResponse<Integer> intResponse = PagedResponse.of(intItems, 1, 2);
        PagedResponse<Long> longResponse = PagedResponse.of(longItems, 1, 2);

        assertThat(intResponse.content()).containsExactly(1, 2);
        assertThat(longResponse.content()).containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("Preserves item order")
    void testPagedResponsePreserveOrder() {
        List<String> allItems = List.of("apple", "banana", "cherry", "date", "elderberry");

        PagedResponse<String> response = PagedResponse.of(allItems, 2, 2);

        assertThat(response.content()).containsExactly("cherry", "date");
    }

    private List<String> createList(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> "item" + i)
                .toList();
    }
}
