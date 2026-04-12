package za.vodacom.repoprofile.adapters.gitlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;
import za.vodacom.repoprofile.exception.ProviderApiException;
import za.vodacom.repoprofile.ports.out.SourceCodeClient;

import java.util.List;

/**
 * Skeleton adapter for GitLab API.
 * <p>
 * Demonstrates that adding a new provider requires only:
 * <ol>
 *   <li>A new adapter class implementing {@link SourceCodeClient}</li>
 *   <li>A {@code @Component} name matching the {@link za.vodacom.repoprofile.domain.model.ProviderType} key</li>
 *   <li>A WebClient bean configured for the provider's base URL</li>
 * </ol>
 * No changes to the service layer, controller, or domain model are required.
 * <p>
 * GitLab API reference: https://docs.gitlab.com/ee/api/users.html
 */
@Component("gitlab")
public class GitLabWebClientAdapter implements SourceCodeClient {

    private static final Logger log = LoggerFactory.getLogger(GitLabWebClientAdapter.class);

    // TODO: Inject a GitLab-specific WebClient bean configured with:
    //   base-url: https://gitlab.com/api/v4
    //   private-token header (optional)

    @Override
    public User fetchUser(String username) {
        log.info("Fetching GitLab profile for user: {}", username);

        // GitLab API: GET /api/v4/users?username={username}
        // Returns array — pick first match
        //
        // Response mapping:
        //   GitLab "username"   → User.login
        //   GitLab "name"       → User.name
        //   GitLab "bio"        → User.bio
        //   GitLab "avatar_url" → User.avatarUrl
        //   GitLab "web_url"    → User.htmlUrl
        //   (GitLab doesn't expose public_repos count directly — requires separate call)

        throw new ProviderApiException("GitLab provider is not yet implemented");
    }

    @Override
    public List<Repo> fetchRepositories(String username) {
        log.info("Fetching GitLab repositories for user: {}", username);

        // GitLab API: GET /api/v4/users/{userId}/projects?order_by=stars&sort=desc
        // Note: Requires user ID (not username) — resolved from fetchUser
        //
        // Response mapping:
        //   GitLab "name"              → Repo.name
        //   GitLab "description"       → Repo.description
        //   GitLab "web_url"           → Repo.htmlUrl
        //   GitLab "star_count"        → Repo.stargazersCount
        //   GitLab "forks_count"       → Repo.forksCount
        //   (language requires separate GET /projects/{id}/languages call)

        throw new ProviderApiException("GitLab provider is not yet implemented");
    }
}
