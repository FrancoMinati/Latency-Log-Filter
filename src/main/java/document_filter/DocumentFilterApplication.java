package document_filter;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import document_filter.files.MetricsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.logging.Logger;


@SpringBootApplication
@EnableScheduling
public class DocumentFilterApplication {

    private static final Logger LOGGER = Logger.getLogger(DocumentFilterApplication.class.getName());

    public static void main(String[] args) {
        // RUN for rest api execution
        // ConfigurableApplicationContext context = SpringApplication.run(DocumentFilterApplication.class, args);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(DocumentFilterApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        LOGGER.info("Proceso finalizado, cerrando app.");
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }

    @Bean
    public CommandLineRunner runOnStartup(MetricsService metricsService) {
        return args -> metricsService.generateDailyMetrics();
    }
}
