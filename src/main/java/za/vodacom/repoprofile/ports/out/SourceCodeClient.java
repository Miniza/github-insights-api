package za.vodacom.repoprofile.ports.out;

import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;

import java.util.List;

/**
 * Driven port – abstraction over any Git hosting provider (GitHub, GitLab, Bitbucket…).
 * Each adapter implements this interface for a specific provider.
 */
public interface SourceCodeClient {

    User fetchUser(String username);

    List<Repo> fetchRepositories(String username);

    /**
     * Fetches a single page of repositories from the provider, sorted by stars descending.
     * Default implementation falls back to fetching all and slicing in memory.
     */
    default List<Repo> fetchRepositories(String username, int page, int perPage) {
        List<Repo> all = fetchRepositories(username);
        int fromIndex = Math.min((page - 1) * perPage, all.size());
        int toIndex = Math.min(fromIndex + perPage, all.size());
        return all.subList(fromIndex, toIndex);
    }
}
