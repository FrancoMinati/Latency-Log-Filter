package document_filter.util;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtil {

    private static final Logger LOGGER = Logger.getLogger(FileUtil.class.getName());

    public static String pathHandle(String filePath) {
        File file = new File(filePath);
        File parentDir = filePath.matches(".*\\.[a-zA-Z0-9]+$") ? file.getParentFile() : file ;

        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                LOGGER.log(Level.SEVERE, "ERROR - Could not create directories: " + parentDir);
            }
        }
        return filePath;
    }

    public static String createPath(Resource resource, String prefix, String suffix) {
        try (InputStream in = resource.getInputStream()) {
            Path tempFile = Files.createTempFile(prefix, suffix);
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile.toString();
        } catch (IOException e) {
            throw new RuntimeException("Could not copy resource to temp file", e);
        }
    }

    public static String getPath(Resource resource) {
        try {
            return resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Borra el contenido de un directorio, incluyendo subdirectorios y archivos.
     */
    public static void clearDirectory(String strPath) {
        try {
            Path dirPath = Paths.get(strPath);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Directorio inválido: " + dirPath);
        }

        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); // borra archivo
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(dirPath)) { // no borramos el directorio raíz
                    Files.delete(dir); // borra subdirectorio
                }
                return FileVisitResult.CONTINUE;
            }
        });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Borra todos los archivos .zip en el directorio que sean más antiguos que 'daysOld'.
     */
    public static void clearOldZips(String strPath, int daysOld) {
            try {
        Path dirPath = Paths.get(strPath);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Directorio inválido: " + dirPath);
        }

        Instant cutoff = Instant.now().minus(daysOld, ChronoUnit.DAYS);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.zip")) {
            for (Path zipFile : stream) {
                BasicFileAttributes attrs = Files.readAttributes(zipFile, BasicFileAttributes.class);
                Instant creationTime = attrs.creationTime().toInstant();
                Instant lastModifiedTime = attrs.lastModifiedTime().toInstant();
                if (creationTime.isBefore(cutoff) || lastModifiedTime.isBefore(cutoff)) {
                    Files.delete(zipFile);
                    LOGGER.info("Borrado ZIP antiguo: " + zipFile.getFileName());
                }
            }
        }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }
}
