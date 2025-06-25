package document_filter.files;

import document_filter.latency.LatencyExcelExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class MetricsService {

    @Value("${metrics.latency.windowSizeSeconds:1}")
    private int windowSizeSeconds;
    @Value("${input.folder.path}")
    private String inputFolderPath;
    @Value("${report.file.path}")
    private String reportFilePath;
    @Value("${summary.file.path}")
    private String summaryFilePath;
    @Value("${output.directory}")
    private String outputDirectory;

    @Scheduled(cron = "0 0 18 * * *", zone = "America/Argentina/Buenos_Aires")
    public void generateDailyMetrics() {
        LatencyExcelExporter.processDirectory(inputFolderPath, windowSizeSeconds, summaryFilePath);
        LatencyExcelExporter.copySummaryToExistingExcel(summaryFilePath, reportFilePath, outputDirectory);
    }
    public InputStreamResource getDailyMetrics() {
        LatencyExcelExporter.processDirectory(inputFolderPath, windowSizeSeconds, summaryFilePath);
        LatencyExcelExporter.copySummaryToExistingExcel(summaryFilePath, reportFilePath, outputDirectory);

        try {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "report-" + date + ".xlsx"; // <-- asegurate que el archivo tenga extensión
            File file = new File(outputDirectory + fileName);

            System.out.println("Buscando archivo: " + file.getAbsolutePath());

            if (!file.exists()) {
                System.err.println("Archivo no encontrado!");
                return null;
            }

            return new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        return null;
    }


}
