package za.vodacom.repoprofile.adapters.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import za.vodacom.repoprofile.domain.event.SearchPerformedEvent;
import za.vodacom.repoprofile.ports.out.SearchHistoryRepositoryPort;

@Component
public class SearchHistoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(SearchHistoryEventListener.class);

    private final SearchHistoryRepositoryPort searchHistoryRepository;
    private final int maxRecords;

    public SearchHistoryEventListener(SearchHistoryRepositoryPort searchHistoryRepository,
                                      @Value("${search-history.max-records:50}") int maxRecords) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.maxRecords = maxRecords;
    }

    @Async
    @EventListener
    public void onSearchPerformed(SearchPerformedEvent event) {
        try {
            searchHistoryRepository.save(event.username(), event.summary());
            searchHistoryRepository.pruneOldest(maxRecords);
            log.debug("Persisted search history for user: {}", event.username());
        } catch (Exception e) {
            log.warn("Failed to persist search history for user: {} — {}", event.username(), e.getMessage());
        }
    }
}
