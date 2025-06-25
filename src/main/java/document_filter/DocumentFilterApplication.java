package document_filter;
import document_filter.latency.LatencyExcelExporter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class DocumentFilterApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentFilterApplication.class, args);

    }
}
