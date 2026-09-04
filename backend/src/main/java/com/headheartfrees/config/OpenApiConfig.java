package com.headheartfrees.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI metadata. Served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI headHeartFreesOpenApi(@Value("${app.version}") String version) {
        return new OpenAPI().info(new Info()
                .title("HeadHeartFreeS API")
                .version(version)
                .description("""
                        Backend for an anonymous emotional-release app.

                        Note on the vent domain: no endpoint accepts vent text. \
                        What a person writes stays in their browser and is never \
                        transmitted. The release endpoint records an optional mood \
                        label and a timestamp only.""")
                .license(new License().name("Proprietary")));
    }
}
