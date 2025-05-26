package document_filter.controller;

import document_filter.files.Metrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final Metrics metrics;

    public MetricsController(Metrics metrics) {
        this.metrics = metrics;
    }
    @Operation(summary = "Exporta métricas diarias a Excel", description = "Genera y retorna un archivo .xlsx con resumen de latencias")
    @ApiResponse(responseCode = "200", description = "Excel generado correctamente")
    @PostMapping("/daily-metrics")
    public ResponseEntity<Resource> getDailyMetrics() {
        InputStreamResource resource = metrics.getDailyMetrics();
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + "daily_metrics.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource); // <-- sin el cast
    }


}
