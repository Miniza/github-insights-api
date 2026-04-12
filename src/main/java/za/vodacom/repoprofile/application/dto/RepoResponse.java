package za.vodacom.repoprofile.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepoResponse(
        String name,
        String description,
        String htmlUrl,
        String language,
        int stargazersCount,
        int forksCount,
        long size
) {
}
