package za.vodacom.repoprofile.ports.out;

import za.vodacom.repoprofile.domain.model.SearchRecord;

import java.util.List;

/**
 * Driven port \u2013 persistence abstraction for search history.
 */
public interface SearchHistoryRepositoryPort {

    SearchRecord save(String username, String summary);

    List<SearchRecord> findRecentSearches(int limit);

    void pruneOldest(int maxRecords);
}
