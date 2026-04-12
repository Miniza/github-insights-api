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
}
