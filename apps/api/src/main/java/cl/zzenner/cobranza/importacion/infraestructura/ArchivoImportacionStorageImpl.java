package cl.zzenner.cobranza.importacion.infraestructura;

import cl.zzenner.cobranza.importacion.aplicacion.ArchivoImportacionStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.UUID;

@Component
class ArchivoImportacionStorageImpl implements ArchivoImportacionStorage {

    private static final Logger log = LoggerFactory.getLogger(ArchivoImportacionStorageImpl.class);

    private final Path baseDir;

    ArchivoImportacionStorageImpl(
            @Value("${app.importacion.storage.directorio:/tmp/importaciones}") String directorio) {
        this.baseDir = Path.of(directorio);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de importaciones: " + directorio, e);
        }
    }

    @Override
    public String guardar(UUID importacionId, InputStream contenido, String nombreOriginal) {
        // Nombre físico siempre por UUID — nunca usar el nombre original como path
        Path dirImportacion = baseDir.resolve(importacionId.toString());
        try {
            Files.createDirectories(dirImportacion);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio para importación " + importacionId, e);
        }

        Path destino = dirImportacion.resolve("archivo.csv");
        try {
            Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Error al guardar el archivo de importación " + importacionId, e);
        }

        return importacionId + "/archivo.csv";
    }

    @Override
    public InputStream leer(String ruta) {
        Path archivo = resolverRuta(ruta);
        try {
            return Files.newInputStream(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo: " + ruta, e);
        }
    }

    @Override
    public void eliminar(String ruta) {
        if (ruta == null) return;
        try {
            Path archivo = resolverRuta(ruta);
            Files.deleteIfExists(archivo);
            Path dir = archivo.getParent();
            if (dir != null && Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    if (stream.findFirst().isEmpty()) {
                        Files.deleteIfExists(dir);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo temporal: {}", ruta, e);
        }
    }

    private Path resolverRuta(String ruta) {
        // Prevenir path traversal
        Path resolved = baseDir.resolve(ruta).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new SecurityException("Ruta de archivo fuera del directorio base: " + ruta);
        }
        return resolved;
    }
}
