package com.matador.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI matadorOpenApi() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("Matador API")
                    .version("v1")
                    .description(
                        "Matador on-demand car rental API. Customer endpoints use JWT bearer "
                            + "tokens; admin endpoints use session cookies."))
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearer-jwt",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Customer JWT access token."))
                    .addSecuritySchemes(
                        "session-cookie",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .in(SecurityScheme.In.COOKIE)
                            .name("SESSION")
                            .description("Admin session cookie.")));
    }
}
