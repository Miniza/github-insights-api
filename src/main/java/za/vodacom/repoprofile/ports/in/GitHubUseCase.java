package za.vodacom.repoprofile.ports.in;

import za.vodacom.repoprofile.application.dto.GitHubProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;

import java.util.List;

/**
 * Driving port – defines all use cases exposed by the application layer.
 */
public interface GitHubUseCase {

    GitHubProfileResponse getProfile(String username);

    List<RepoResponse> getRepositories(String username);

    List<SearchSummary> getSearchHistory();
}
