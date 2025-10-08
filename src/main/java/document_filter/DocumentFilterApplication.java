package document_filter;
import document_filter.files.MetricsService;
import document_filter.latency.LatencyExcelExporter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class DocumentFilterApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DocumentFilterApplication.class, args);

        int exitCode = SpringApplication.exit(context);
        System.out.println("Proceso finalizado, cerrando app.");
        System.exit(exitCode);
    }

    @Bean
    public CommandLineRunner runOnStartup(MetricsService metricsService) {
        return args -> metricsService.generateDailyMetrics();
    }
}
