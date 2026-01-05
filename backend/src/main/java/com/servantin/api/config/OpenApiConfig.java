package com.servantin.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Serv@nitin API")
                        .version("1.0.0")
                        .description(
                                """
                                        API for Serv@nitin - An Uber-style marketplace for services in French-speaking Switzerland.

                                        ## Authentication
                                        Most endpoints require a JWT token obtained from `/api/auth/login`.
                                        Include the token in the Authorization header: `Bearer <token>`

                                        ## Service Categories
                                        The platform supports 7 service categories:
                                        1. Babysitting & Nanny
                                        2. Home Support & Handyman
                                        3. Disability & Healthcare Assistance
                                        4. Tax & Administration Support
                                        5. Entrepreneur & Startup Support
                                        6. Travel, Visa & Booking Assistance
                                        7. Elderly Support & At-Home Assistance
                                        """)
                        .contact(new Contact()
                                .name("Serv@nitin Support")
                                .email("support@servantin.ch"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://servantin.ch/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local development"),
                        new Server().url("https://api.servantin.ch").description("Production")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token obtained from /api/auth/login")));
    }
}
