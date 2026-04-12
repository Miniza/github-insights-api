package za.vodacom.repoprofile.domain.model;

/**
 * Supported source-code hosting providers.
 */
public enum ProviderType {

    GITHUB("github"),
    GITLAB("gitlab"),
    BITBUCKET("bitbucket");

    private final String key;

    ProviderType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Resolves a ProviderType from a case-insensitive string key.
     *
     * @throws IllegalArgumentException if the provider is not supported
     */
    public static ProviderType from(String value) {
        for (ProviderType type : values()) {
            if (type.key.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported provider: '%s'. Supported: github, gitlab, bitbucket".formatted(value));
    }
}
