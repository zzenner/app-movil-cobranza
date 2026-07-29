package cl.zzenner.cobranza.autenticacion.seguridad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

class CargadorClavesRsa {

    private CargadorClavesRsa() {}

    static RSAPublicKey cargarPublica(String rutaArchivo) {
        try {
            String pem = Files.readString(Path.of(rutaArchivo));
            byte[] der = decodePem(pem, "PUBLIC KEY");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(der));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer clave pública RSA desde: " + rutaArchivo, e);
        } catch (Exception e) {
            throw new IllegalStateException("Clave pública RSA inválida: " + rutaArchivo, e);
        }
    }

    static RSAPrivateKey cargarPrivada(String rutaArchivo) {
        try {
            String pem = Files.readString(Path.of(rutaArchivo));
            byte[] der = decodePem(pem, "PRIVATE KEY");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer clave privada RSA desde: " + rutaArchivo, e);
        } catch (Exception e) {
            throw new IllegalStateException("Clave privada RSA inválida: " + rutaArchivo, e);
        }
    }

    private static byte[] decodePem(String pem, String tipo) {
        String limpio = pem
                .replace("-----BEGIN " + tipo + "-----", "")
                .replace("-----END " + tipo + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(limpio);
    }
}
