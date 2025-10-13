package report_generator.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Component
public class RelacionBeMixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelacionBeMixService.class);

    @Value("${relacion-be-mix.folder.path}")
    private String inputFolderPath;

    @Value("${output.folder.path}")
    private String outputFolderPath;

    public void processTodayFile() {
        try {
            Path inputDir = Paths.get(inputFolderPath);
            Path outputDir = Paths.get(outputFolderPath);

            if (!Files.exists(inputDir)) {
                LOGGER.error("Input directory not found: {}", inputFolderPath);
                return;
            }

            // 1️⃣ Buscar el primer archivo con fecha del día de hoy
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Optional<File> todayFile = Arrays.stream(Optional.ofNullable(inputDir.toFile().listFiles()).orElse(new File[0]))
                    .filter(File::isFile)
                    .filter(f -> f.getName().contains(today))
                    .min(Comparator.comparingLong(File::lastModified));

            if (!todayFile.isPresent()) {
                LOGGER.warn("No se encontró ningún archivo con fecha de hoy ({}) en {}", today, inputFolderPath);
                return;
            }

            File sourceFile = todayFile.get();
            LOGGER.info("Archivo encontrado: {}", sourceFile.getName());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String newFileName = timestamp + "_relaciones_be_mix.txt";
            Path targetFile = outputDir.resolve(newFileName);

            Files.copy(sourceFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            FileTime now = FileTime.from(Instant.now());
            Files.setAttribute(targetFile, "creationTime", now);
            Files.setAttribute(targetFile, "lastModifiedTime", now);
            LOGGER.info("Archivo copiado exitosamente a {}", targetFile);

        } catch (IOException e) {
            LOGGER.error("Error al procesar archivo diario", e);
        }
    }
}
