package za.vodacom.repoprofile.util;

public final class Constants {

    private Constants() {
    }

    // Cache names (provider-agnostic; each adapter prefixes the key with its provider name)
    public static final String CACHE_PROFILES = "profiles";
    public static final String CACHE_REPOS = "repos";
}
