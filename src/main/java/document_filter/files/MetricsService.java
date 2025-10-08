package document_filter.files;

import document_filter.latency.LatencyExcelExporter;
import document_filter.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class MetricsService {

    private static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());

    @Value("${metrics.latency.windowSizeSeconds:1}")
    private int windowSizeSeconds;
    @Value("${input.folder.path}")
    private String inputFolderPath;
    @Value("${report.file.path}")
    private Resource reportFilePath;
    @Value("${temp.folder.path}")
    private String tempFolderPath;
    @Value("${summary.file.path}")
    private String summaryFilePath;
    @Value("${output.folder.path}")
    private String outputDirectory;

    private final RelacionBeMixService relacionBeMixService;
    private final CopyInputsService copyInputsService;

    @Autowired
    public MetricsService(RelacionBeMixService relacionBeMixService, CopyInputsService copyInputsService) {
        this.relacionBeMixService = relacionBeMixService;
        this.copyInputsService = copyInputsService;
    }

    //    @Scheduled(cron = "00 15 17 * * *", zone = "America/Argentina/Buenos_Aires")
    public void cronedExecution() {
        generateDailyMetrics();
    }

    public void generateDailyMetrics() {
        FileUtil.clearDirectory(tempFolderPath);
        copyInputsService.copyDelayLogs(inputFolderPath,FileUtil.pathHandle(tempFolderPath), FileUtil.pathHandle(outputDirectory));
        LatencyExcelExporter.processDirectory(tempFolderPath, windowSizeSeconds, FileUtil.pathHandle(summaryFilePath));
        LatencyExcelExporter.copySummaryToExistingExcel(summaryFilePath, FileUtil.getPath(reportFilePath), outputDirectory);
        relacionBeMixService.processTodayFile();
        FileUtil.clearDirectory(tempFolderPath);
        FileUtil.clearOldZips(outputDirectory,7);
    }

    public InputStreamResource getDailyMetrics() {
        generateDailyMetrics();

        try {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "report-" + date + ".xlsx"; // <-- asegurate que el archivo tenga extensión
            File file = new File(outputDirectory + fileName);

            LOGGER.info("Buscando archivo: " + file.getAbsolutePath());

            if (!file.exists()) {
                LOGGER.log(Level.SEVERE, "Archivo no encontrado!");
                return null;
            }

            return new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, ex.toString());
        }

        return null;
    }


}
