package document_filter.files;

import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class CopyInputsService {

    private static final Logger LOGGER = Logger.getLogger(CopyInputsService.class.getName());
    private static final String TARGET_FILENAME = "delay.log";

    /**
     * Recorre los directorios de primer nivel en rootDir y copia delay.log (si existe)
     * dentro de destDir/<nombre_directorio_primer_nivel>/delay.log
     *
     * @param rootDirPath ruta de la carpeta raíz (String)
     * @param tempPath ruta de la carpeta de procesamiento (String)
     * @param outputPath ruta de la carpeta destino (String)
     * @throws IOException si ocurre un error de E/S mayor
     */
    public void copyDelayLogs(String rootDirPath, String tempPath, String outputPath) {
        if (rootDirPath == null || rootDirPath.trim().isEmpty()) {
            throw new IllegalArgumentException("El parámetro rootDirPath no puede ser nulo ni vacío.");
        }
        if (tempPath == null || tempPath.trim().isEmpty()) {
            throw new IllegalArgumentException("El parámetro destDirPath no puede ser nulo ni vacío.");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("El parámetro destDirPath no puede ser nulo ni vacío.");
        }

        Path rootDir = Paths.get(rootDirPath).toAbsolutePath().normalize();
        Path destDir = Paths.get(tempPath).toAbsolutePath().normalize();
        Path outputDir = Paths.get(outputPath).toAbsolutePath().normalize();

        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("El directorio raíz no existe o no es válido: " + rootDir);
        }

        try (Stream<Path> stream = Files.list(rootDir)) {
            stream.filter(Files::isDirectory).forEach(firstLevelDir -> {
                Path sourceDelay = firstLevelDir.resolve(TARGET_FILENAME);
                if (Files.exists(sourceDelay) && Files.isRegularFile(sourceDelay)) {
                    Path targetDir = destDir.resolve(firstLevelDir.getFileName());
                    try {
                        if (!Files.exists(targetDir)) {
                            Files.createDirectories(targetDir);
                        }
                        Path targetFile = targetDir.resolve(TARGET_FILENAME);
                        Files.copy(sourceDelay, targetFile,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
//                        LOGGER.info(String.format("Copiado: %s -> %s", sourceDelay, targetFile));
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error copiando " + sourceDelay + " a " + targetDir, e);
                    }
                } else {
                    LOGGER.info("No se encontró " + TARGET_FILENAME + " en " + firstLevelDir);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Crear ZIP al final del proceso
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path zipFile = outputDir.resolve(timestamp + "_inputs.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toFile().toPath()))) {
            Files.walkFileTree(destDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Evitar agregar el ZIP dentro de sí mismo
                    if (!file.equals(zipFile)) {
                        Path relativePath = destDir.relativize(file);
                        zos.putNextEntry(new ZipEntry(relativePath.toString().replace("\\", "/")));
                        Files.copy(file, zos);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            LOGGER.info("Archivo ZIP creado: " + zipFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creando ZIP en " + zipFile, e);
        }
    }
}
