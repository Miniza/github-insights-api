package za.vodacom.repoprofile.application.mapper;

import za.vodacom.repoprofile.application.dto.ProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.SearchRecord;
import za.vodacom.repoprofile.domain.model.User;

import java.time.ZoneId;
import java.util.List;

public final class ProfileMapper {

    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

    private ProfileMapper() {
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

    public static ProfileResponse toProfileResponse(User user,
                                                     String topLanguage,
                                                     List<RepoResponse> repos) {
        return new ProfileResponse(
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

    public static SearchSummary toSearchSummary(SearchRecord record) {
        return new SearchSummary(
                record.id(),
                record.username(),
                record.summary(),
                record.searchedAt().atZone(SAST)
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
