package za.vodacom.repoprofile.domain.model;

public record User(
        String login,
        String name,
        String bio,
        String avatarUrl,
        String htmlUrl,
        int publicRepos,
        int followers,
        int following
) {
}
