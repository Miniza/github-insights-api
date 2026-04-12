package za.vodacom.repoprofile.application.mapper;

import za.vodacom.repoprofile.adapters.persistence.entity.SearchHistoryEntity;
import za.vodacom.repoprofile.application.dto.GitHubProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;

import java.util.List;

public final class GitHubMapper {

    private GitHubMapper() {
    }

    public static RepoResponse toRepoResponse(Repo repo) {
        return new RepoResponse(
                repo.name(),
                repo.description(),
                repo.htmlUrl(),
                repo.language(),
                repo.stargazersCount(),
                repo.forksCount(),
                repo.size()
        );
    }

    public static GitHubProfileResponse toProfileResponse(User user,
                                                           String topLanguage,
                                                           List<RepoResponse> repos) {
        return new GitHubProfileResponse(
                user.login(),
                user.name(),
                user.bio(),
                user.avatarUrl(),
                user.htmlUrl(),
                user.publicRepos(),
                user.followers(),
                user.following(),
                topLanguage,
                repos
        );
    }

    public static SearchSummary toSearchSummary(SearchHistoryEntity entity) {
        return new SearchSummary(
                entity.getId(),
                entity.getUsername(),
                entity.getSummary(),
                entity.getSearchedAt()
        );
    }

    public static String buildSummary(String username, int repoCount, String topLanguage) {
        return "%s – %d repos – %s".formatted(
                username,
                repoCount,
                topLanguage != null ? topLanguage : "N/A"
        );
    }
}
