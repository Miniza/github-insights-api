package za.vodacom.repoprofile.ports.out;

import za.vodacom.repoprofile.adapters.persistence.entity.SearchHistoryEntity;

import java.util.List;

/**
 * Driven port – persistence abstraction for search history.
 */
public interface SearchHistoryRepositoryPort {

    SearchHistoryEntity save(String username, String summary);

    List<SearchHistoryEntity> findRecentSearches(int limit);

    void pruneOldest(int maxRecords);
}
