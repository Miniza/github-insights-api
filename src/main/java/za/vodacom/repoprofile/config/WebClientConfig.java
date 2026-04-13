package za.vodacom.repoprofile.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient gitHubWebClient(
            WebClient.Builder builder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.user-agent}") String userAgent,
            @Value("${github.api.token:}") String token,
            @Value("${github.api.connect-timeout:5s}") Duration connectTimeout,
            @Value("${github.api.read-timeout:10s}") Duration readTimeout) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .responseTimeout(readTimeout);

        WebClient.Builder configured = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent);

        if (token != null && !token.isBlank()) {
            configured = configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return configured.build();
    }
}
