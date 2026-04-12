package za.vodacom.repoprofile.adapters.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.vodacom.repoprofile.adapters.persistence.entity.SearchHistoryEntity;

import java.util.List;

public interface SpringDataSearchRepository extends JpaRepository<SearchHistoryEntity, Long> {

    List<SearchHistoryEntity> findAllByOrderBySearchedAtDesc();

    @Modifying
    @Query("""
            DELETE FROM SearchHistoryEntity s
            WHERE s.id NOT IN (
                SELECT s2.id FROM SearchHistoryEntity s2
                ORDER BY s2.searchedAt DESC
                LIMIT :maxRecords
            )
            """)
    void deleteOldestBeyond(@Param("maxRecords") int maxRecords);
}
