package report_generator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import report_generator.files.MetricsService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Operation(summary = "Exporta métricas diarias a Excel (GET alias)", description = "Permite generar métricas diarias con una solicitud GET en lugar de POST")
    @ApiResponse(responseCode = "200", description = "Excel generado correctamente")
    @GetMapping("/generate-and-download-daily-report")
    public ResponseEntity<Resource> getDailyMetricsGet() {
        return getDailyMetrics();
    }

    @Operation(summary = "Exporta métricas diarias a Excel", description = "Genera y retorna un archivo .xlsx con resumen de latencias")
    @ApiResponse(responseCode = "200", description = "Excel generado correctamente")
    @PostMapping("/generate-and-download-daily-report")
    public ResponseEntity<Resource> getDailyMetrics() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = "report-" + date + ".xlsx";

        InputStreamResource resource = metricsService.getDailyMetrics();
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
