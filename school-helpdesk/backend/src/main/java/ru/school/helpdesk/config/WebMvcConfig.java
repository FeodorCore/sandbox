package ru.school.helpdesk.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Раздача собранного React-приложения из {@code classpath:/static}
 * с поддержкой client-side routing: любой неизвестный путь (например {@code /tickets/42})
 * отдаёт {@code index.html}, а маршруты {@code /api/**} и {@code /h2-console/**} не перехватываются.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    private static class SpaResourceResolver extends PathResourceResolver {

        private final Resource index = new ClassPathResource("static/index.html");

        @Override
        protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            // API и служебные пути не должны попадать в SPA-фолбэк
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("h2-console")
                    || resourcePath.startsWith("v3/") || resourcePath.startsWith("swagger")) {
                return null;
            }
            return index.exists() ? index : null;
        }
    }
}


