package za.vodacom.repoprofile.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GitHub Profile Insights API")
                        .description("A production-ready API that consumes the GitHub REST API to provide user profile insights, repository listings, and search history.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Repo Profile Team")));
    }
}
