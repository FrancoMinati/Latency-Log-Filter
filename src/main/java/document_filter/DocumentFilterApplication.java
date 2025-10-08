package document_filter;
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
        ConfigurableApplicationContext context = SpringApplication.run(DocumentFilterApplication.class, args);

        LOGGER.info("Proceso finalizado, cerrando app.");
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }

    @Bean
    public CommandLineRunner runOnStartup(MetricsService metricsService) {
        return args -> metricsService.generateDailyMetrics();
    }
}
