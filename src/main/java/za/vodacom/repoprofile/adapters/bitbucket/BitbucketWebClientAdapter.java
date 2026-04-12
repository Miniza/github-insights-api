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
 * Skeleton adapter for Bitbucket Cloud API.
 * <p>
 * Demonstrates that adding a new provider requires only:
 * <ol>
 *   <li>A new adapter class implementing {@link SourceCodeClient}</li>
 *   <li>A {@code @Component} name matching the {@link za.vodacom.repoprofile.domain.model.ProviderType} key</li>
 *   <li>A WebClient bean configured for the provider's base URL</li>
 * </ol>
 * No changes to the service layer, controller, or domain model are required.
 * <p>
 * Bitbucket API reference: https://developer.atlassian.com/cloud/bitbucket/rest/api-group-users/
 */
@Component("bitbucket")
public class BitbucketWebClientAdapter implements SourceCodeClient {

    private static final Logger log = LoggerFactory.getLogger(BitbucketWebClientAdapter.class);

    // TODO: Inject a Bitbucket-specific WebClient bean configured with:
    //   base-url: https://api.bitbucket.org/2.0
    //   OAuth 2.0 or App password header (optional)

    @Override
    public User fetchUser(String username) {
        log.info("Fetching Bitbucket profile for user: {}", username);

        // Bitbucket API: GET /2.0/users/{username}
        //
        // Response mapping:
        //   Bitbucket "username"     → User.login
        //   Bitbucket "display_name" → User.name
        //   (Bitbucket has no bio field)
        //   Bitbucket "links.avatar.href" → User.avatarUrl
        //   Bitbucket "links.html.href"   → User.htmlUrl
        //   (public_repos requires separate call to /2.0/repositories/{username})

        throw new ProviderApiException("Bitbucket provider is not yet implemented");
    }

    @Override
    public List<Repo> fetchRepositories(String username) {
        log.info("Fetching Bitbucket repositories for user: {}", username);

        // Bitbucket API: GET /2.0/repositories/{username}?sort=-updated_on
        // Note: Bitbucket uses paginated responses with "values" array
        //
        // Response mapping:
        //   Bitbucket "name"                → Repo.name
        //   Bitbucket "description"         → Repo.description
        //   Bitbucket "links.html.href"     → Repo.htmlUrl
        //   Bitbucket "language"            → Repo.language
        //   (Bitbucket has no star count — no direct equivalent)
        //   (forks via /2.0/repositories/{workspace}/{slug}/forks — separate call)
        //   Bitbucket "size"                → Repo.size

        throw new ProviderApiException("Bitbucket provider is not yet implemented");
    }
}
