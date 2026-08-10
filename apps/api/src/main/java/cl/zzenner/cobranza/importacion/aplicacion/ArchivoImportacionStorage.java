package cl.zzenner.cobranza.importacion.aplicacion;

import java.io.InputStream;
import java.util.UUID;

public interface ArchivoImportacionStorage {

    String guardar(UUID importacionId, InputStream contenido, String nombreOriginal);

    InputStream leer(String ruta);

    void eliminar(String ruta);
}
