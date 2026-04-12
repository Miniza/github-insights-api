package za.vodacom.repoprofile.adapters.persistence;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.vodacom.repoprofile.adapters.persistence.entity.SearchHistoryEntity;
import za.vodacom.repoprofile.ports.out.SearchHistoryRepositoryPort;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
public class SearchHistoryRepositoryAdapter implements SearchHistoryRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(SearchHistoryRepositoryAdapter.class);

    private final SpringDataSearchRepository repository;

    public SearchHistoryRepositoryAdapter(SpringDataSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "database", fallbackMethod = "saveFallback")
    public SearchHistoryEntity save(String username, String summary) {
        SearchHistoryEntity entity = new SearchHistoryEntity(username, summary, Instant.now());
        return repository.save(entity);
    }

    @Override
    @CircuitBreaker(name = "database", fallbackMethod = "findRecentSearchesFallback")
    public List<SearchHistoryEntity> findRecentSearches(int limit) {
        return repository.findAllByOrderBySearchedAtDesc().stream()
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "database", fallbackMethod = "pruneOldestFallback")
    public void pruneOldest(int maxRecords) {
        repository.deleteOldestBeyond(maxRecords);
    }

    @SuppressWarnings("unused")
    private SearchHistoryEntity saveFallback(String username, String summary, Throwable t) {
        log.error("Database circuit breaker open – could not save search history for '{}': {}", username, t.getMessage());
        return new SearchHistoryEntity(username, summary, Instant.now());
    }

    @SuppressWarnings("unused")
    private List<SearchHistoryEntity> findRecentSearchesFallback(int limit, Throwable t) {
        log.error("Database circuit breaker open – returning empty search history: {}", t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private void pruneOldestFallback(int maxRecords, Throwable t) {
        log.error("Database circuit breaker open – skipping prune: {}", t.getMessage());
    }
}
