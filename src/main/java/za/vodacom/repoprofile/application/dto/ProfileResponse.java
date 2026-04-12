package za.vodacom.repoprofile.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
        String login,
        String name,
        String bio,
        String avatarUrl,
        String htmlUrl,
        int publicRepos,
        int followers,
        int following,
        String topLanguage,
        List<RepoResponse> repositories
) {
}
