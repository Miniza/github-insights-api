package za.vodacom.repoprofile.ports.out;

import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;

import java.util.List;

/**
 * Driven port – abstraction over any Git hosting provider (GitHub, GitLab, Bitbucket…).
 */
public interface GitHubClient {

    User fetchUser(String username);

    List<Repo> fetchRepositories(String username);
}
