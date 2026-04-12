package za.vodacom.repoprofile.adapters.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "search_history")
public class SearchHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String summary;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    protected SearchHistoryEntity() {
    }

    public SearchHistoryEntity(String username, String summary, Instant searchedAt) {
        this.username = username;
        this.summary = summary;
        this.searchedAt = searchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getSearchedAt() {
        return searchedAt;
    }
}
