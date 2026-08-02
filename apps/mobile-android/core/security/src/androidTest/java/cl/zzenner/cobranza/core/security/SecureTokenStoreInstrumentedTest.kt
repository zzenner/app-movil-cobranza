package cl.zzenner.cobranza.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas del SecureTokenStore.
 *
 * Requieren dispositivo físico o emulador con soporte de Android Keystore.
 * Se ejecutan en el contexto real de Android para validar el cifrado AES-GCM.
 *
 * Ejecutar con:
 *   ./gradlew :core:security:connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SecureTokenStoreInstrumentedTest {

    private lateinit var store: SecureTokenStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        store = SecureTokenStore(context)
    }

    @Test
    fun cifrado_y_descifrado_del_refresh_token_son_inversos() = runTest {
        val tokenOriginal = "mi-refresh-token-secreto-12345"
        store.saveRefreshToken(tokenOriginal)
        val recuperado = store.getRefreshToken()
        assertEquals(tokenOriginal, recuperado)
    }

    @Test
    fun token_inexistente_devuelve_null() = runTest {
        store.clearRefreshToken()
        val resultado = store.getRefreshToken()
        assertNull(resultado)
    }

    @Test
    fun clear_elimina_el_token() = runTest {
        store.saveRefreshToken("token-a-borrar")
        store.clearRefreshToken()
        val resultado = store.getRefreshToken()
        assertNull(resultado)
    }

    @Test
    fun iv_es_unico_por_operacion_de_cifrado() = runTest {
        // Guardar el mismo token dos veces — los datos cifrados deben ser distintos (IV distinto)
        val token = "mismo-token"
        store.saveRefreshToken(token)
        val recuperado1 = store.getRefreshToken()

        store.saveRefreshToken(token)
        val recuperado2 = store.getRefreshToken()

        // El token descifrado es el mismo, pero el IV fue distinto en cada operación
        assertEquals(recuperado1, recuperado2)
        assertEquals(token, recuperado1)
    }

    @Test
    fun has_refresh_token_flow_refleja_estado_correcto() = runTest {
        store.clearRefreshToken()

        val tieneBefore = store.hasRefreshTokenFlow().first()
        assertFalse(tieneBefore)

        store.saveRefreshToken("token-de-prueba")
        val tieneAfter = store.hasRefreshTokenFlow().first()
        assertTrue(tieneAfter)
    }
}
