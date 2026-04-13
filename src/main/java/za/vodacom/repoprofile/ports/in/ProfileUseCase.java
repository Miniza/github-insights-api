package za.vodacom.repoprofile.ports.in;

import za.vodacom.repoprofile.application.dto.ProfileResponse;
import za.vodacom.repoprofile.application.dto.PagedResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;

import java.util.List;

/**
 * Use cases exposed by the application layer.
 * The {@code provider} parameter selects the source (github, gitlab, bitbucket).
 */
public interface ProfileUseCase {

    ProfileResponse getProfile(String username, String provider);

    PagedResponse<RepoResponse> getRepositories(String username, String provider, int page, int perPage);

    List<SearchSummary> getSearchHistory();
}
