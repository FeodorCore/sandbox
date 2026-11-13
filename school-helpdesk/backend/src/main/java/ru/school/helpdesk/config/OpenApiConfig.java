package ru.school.helpdesk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Описание REST API для Swagger UI (доступен в профиле dev для роли ADMIN). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI helpDeskOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("School HelpDesk API")
                        .version("1.0.0")
                        .description("REST API системы управления техническими заявками школы. "
                                + "Для доступа выполните POST /api/auth/login и добавьте полученный токен "
                                + "через кнопку Authorize.")
                        .contact(new Contact().name("Служба технической поддержки школы")))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}


