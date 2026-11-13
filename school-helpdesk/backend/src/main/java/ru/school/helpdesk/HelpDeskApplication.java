package ru.school.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * School HelpDesk — система управления техническими заявками школы.
 *
 * <p>Backend одновременно выполняет две роли:
 * <ul>
 *   <li>REST API для фронтенда (пакет {@code web});</li>
 *   <li>веб-сервер, раздающий собранный React-билд из {@code classpath:/static}.</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
public class HelpDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpDeskApplication.class, args);
    }
}


