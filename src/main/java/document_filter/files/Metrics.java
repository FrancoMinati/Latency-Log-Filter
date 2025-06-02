package document_filter.files;

import document_filter.latency.LatencyExcelExporter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class Metrics {

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

    //@PostConstruct
    public void exportMetrics() {
        System.out.println("Procesando archivos de latencia...");

//        LatencyExcelExporter.processDirectory(inputFolderPath, windowSizeSeconds, summaryFilePath);
//        LatencyExcelExporter.copySummaryToExistingExcel(summaryFilePath, reportFilePath);
    }

    public InputStreamResource getDailyMetrics() {

        LatencyExcelExporter.processDirectory(inputFolderPath, windowSizeSeconds, summaryFilePath);
        LatencyExcelExporter.copySummaryToExistingExcel(summaryFilePath, reportFilePath, outputDirectory);
        try {
            String date= LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // Leer el archivo generado como recurso
            File file = new File(outputDirectory+"report-"+date);
            return new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException eX) {
            System.out.println(eX.getCause());
        }

        return null;
    }
}
