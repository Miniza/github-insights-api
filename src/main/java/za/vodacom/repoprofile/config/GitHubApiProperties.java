package za.vodacom.repoprofile.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "github.api")
public record GitHubApiProperties(

        @NotBlank(message = "github.api.base-url is required")
        String baseUrl,

        @NotBlank(message = "github.api.user-agent is required")
        String userAgent,

        String token,

        @NotNull(message = "github.api.connect-timeout is required")
        Duration connectTimeout,

        @NotNull(message = "github.api.read-timeout is required")
        Duration readTimeout,

        @Min(value = 1, message = "github.api.max-pages must be at least 1")
        int maxPages,

        @Min(value = 1, message = "github.api.per-page must be at least 1")
        int perPage
) {
}
