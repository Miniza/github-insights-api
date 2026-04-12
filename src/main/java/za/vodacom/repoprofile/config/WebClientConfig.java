package za.vodacom.repoprofile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient gitHubWebClient(
            WebClient.Builder builder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.user-agent}") String userAgent,
            @Value("${github.api.token:}") String token) {

        WebClient.Builder configured = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent);

        if (token != null && !token.isBlank()) {
            configured = configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return configured.build();
    }
}
