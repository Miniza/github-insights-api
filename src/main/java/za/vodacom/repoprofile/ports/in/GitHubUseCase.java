package za.vodacom.repoprofile.ports.in;

import za.vodacom.repoprofile.application.dto.GitHubProfileResponse;
import za.vodacom.repoprofile.application.dto.PagedResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;

import java.util.List;

/**
 * Driving port – defines all use cases exposed by the application layer.
 * Provider-agnostic: the {@code provider} parameter selects the source (github, gitlab, bitbucket).
 */
public interface GitHubUseCase {

    GitHubProfileResponse getProfile(String username, String provider);

    PagedResponse<RepoResponse> getRepositories(String username, String provider, int page, int perPage);

    List<SearchSummary> getSearchHistory();
}
