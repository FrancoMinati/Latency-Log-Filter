package document_filter.files;

import document_filter.latency.LatencyExcelExporter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    @PostConstruct
    public void exportMetrics(){
        System.out.println("Procesando archivos de latencia...");

        LatencyExcelExporter.processDirectory(inputFolderPath, windowSizeSeconds, summaryFilePath);
        LatencyExcelExporter.copySummaryToExistingExcel(summaryFilePath,reportFilePath);
    }
}
