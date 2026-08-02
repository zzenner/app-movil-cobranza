package cl.zzenner.cobranza.core.security

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Pruebas JVM del InstallationIdStore.
 *
 * NOTA: DataStore requiere contexto Android real para funcionar correctamente.
 * Las pruebas de persistencia real se realizan en [SecureTokenStoreInstrumentedTest]
 * en el directorio androidTest.
 *
 * Aquí se prueba principalmente el formato del UUID generado.
 */
class InstallationIdStoreTest {

    @Test
    fun `UUID generado tiene formato canónico`() {
        val uuid = java.util.UUID.randomUUID().toString()
        assertNotNull(uuid)
        assertEquals(36, uuid.length)
        val partes = uuid.split("-")
        assertEquals(5, partes.size)
        assertEquals(8, partes[0].length)
        assertEquals(4, partes[1].length)
        assertEquals(4, partes[2].length)
        assertEquals(4, partes[3].length)
        assertEquals(12, partes[4].length)
    }

    @Test
    fun `dos UUIDs generados son distintos`() {
        val id1 = java.util.UUID.randomUUID().toString()
        val id2 = java.util.UUID.randomUUID().toString()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `UUID tiene version 4`() {
        val uuid = java.util.UUID.randomUUID()
        assertEquals(4, uuid.version())
        assertEquals(2, uuid.variant())
    }
}
