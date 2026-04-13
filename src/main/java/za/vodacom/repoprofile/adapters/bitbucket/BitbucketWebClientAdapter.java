package za.vodacom.repoprofile.adapters.bitbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;
import za.vodacom.repoprofile.exception.ProviderApiException;
import za.vodacom.repoprofile.ports.out.SourceCodeClient;

import java.util.List;

/**
 * Skeleton adapter for Bitbucket Cloud.
 * Not yet implemented — wired as a {@code @Component("bitbucket")} so the
 * resolver can discover it once a Bitbucket WebClient bean is configured.
 */
@Component("bitbucket")
public class BitbucketWebClientAdapter implements SourceCodeClient {

    private static final Logger log = LoggerFactory.getLogger(BitbucketWebClientAdapter.class);

    @Override
    public User fetchUser(String username) {
        log.info("Fetching Bitbucket profile for user: {}", username);
        throw new ProviderApiException("Bitbucket provider is not yet implemented");
    }

    @Override
    public List<Repo> fetchRepositories(String username) {
        log.info("Fetching Bitbucket repositories for user: {}", username);
        throw new ProviderApiException("Bitbucket provider is not yet implemented");
    }
}
