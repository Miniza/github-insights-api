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
 * Skeleton adapter for GitLab.
 * Not yet implemented — wired as a {@code @Component("gitlab")} so the
 * {@link za.vodacom.repoprofile.ports.out.SourceCodeClient} resolver can
 * pick it up once a GitLab WebClient bean is configured.
 */
@Component("gitlab")
public class GitLabWebClientAdapter implements SourceCodeClient {

    private static final Logger log = LoggerFactory.getLogger(GitLabWebClientAdapter.class);

    @Override
    public User fetchUser(String username) {
        log.info("Fetching GitLab profile for user: {}", username);
        throw new ProviderApiException("GitLab provider is not yet implemented");
    }

    @Override
    public List<Repo> fetchRepositories(String username) {
        log.info("Fetching GitLab repositories for user: {}", username);
        throw new ProviderApiException("GitLab provider is not yet implemented");
    }
}
