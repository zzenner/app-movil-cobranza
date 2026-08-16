package cl.zzenner.cobranza.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cobranza_prefs")

/**
 * Persiste el identificadorInstalacion (UUID v4) y datos no sensibles de sesión
 * en Preferences DataStore (texto plano cifrado a nivel de sistema operativo en Android 10+).
 *
 * El identificadorInstalacion:
 * - Se genera una sola vez al primer acceso.
 * - Sobrevive reinicios de la aplicación.
 * - Identifica la instalación ante la API.
 * - NO es el UUID interno de la tabla dispositivos.
 */
@Singleton
class InstallationIdStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    private object Keys {
        val INSTALLATION_ID = stringPreferencesKey("installation_id")
        val SESSION_EXPIRES_AT = stringPreferencesKey("session_expires_at")
        val NOMBRE_USUARIO = stringPreferencesKey("nombre_usuario")
    }

    /**
     * Devuelve el identificadorInstalacion existente o genera uno nuevo.
     * Garantiza que solo se genera una vez (idempotente).
     */
    suspend fun getOrCreateInstallationId(): String {
        val stored = dataStore.data.map { it[Keys.INSTALLATION_ID] }.first()
        if (stored != null) return stored

        val nuevo = UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[Keys.INSTALLATION_ID] = nuevo }
        return nuevo
    }

    fun sessionExpiresAtFlow(): Flow<String?> =
        dataStore.data.map { it[Keys.SESSION_EXPIRES_AT] }

    suspend fun saveSessionExpiresAt(expiresAt: String) {
        dataStore.edit { prefs -> prefs[Keys.SESSION_EXPIRES_AT] = expiresAt }
    }

    fun nombreUsuarioFlow(): Flow<String?> =
        dataStore.data.map { it[Keys.NOMBRE_USUARIO] }

    suspend fun saveNombreUsuario(nombreUsuario: String) {
        dataStore.edit { prefs -> prefs[Keys.NOMBRE_USUARIO] = nombreUsuario }
    }

    suspend fun clearSessionData() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SESSION_EXPIRES_AT)
            prefs.remove(Keys.NOMBRE_USUARIO)
        }
    }
}
