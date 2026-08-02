package cl.zzenner.cobranza.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secureStore by preferencesDataStore(name = "cobranza_secure")

/**
 * Almacena el refresh token cifrado con AES-256-GCM protegido por Android Keystore.
 *
 * Mecanismo:
 * - La clave AES es generada y gestionada por Android Keystore (nunca sale del hardware/TEE).
 * - Cada operación de cifrado usa un IV único de 12 bytes (GCM estándar).
 * - El ciphertext y el IV se persisten juntos codificados en Base64URL.
 * - El acceso token se mantiene SOLO en memoria (no persiste aquí).
 *
 * No se usa EncryptedSharedPreferences (deprecado en API 34+ para claves de seguridad).
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.secureStore
    private val KEYSTORE = "AndroidKeyStore"
    private val KEY_ALIAS = "cobranza_refresh_token_key"
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val GCM_IV_LENGTH = 12
    private val GCM_TAG_LENGTH = 128

    private object Keys {
        val ENCRYPTED_REFRESH_TOKEN = stringPreferencesKey("enc_refresh_token")
        val REFRESH_TOKEN_IV = stringPreferencesKey("enc_refresh_iv")
    }

    /**
     * Guarda el refresh token cifrado. Operación atómica: ciphertext e IV
     * se persisten en la misma transacción DataStore.
     */
    suspend fun saveRefreshToken(plainToken: String) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainToken.toByteArray(Charsets.UTF_8))

        dataStore.edit { prefs ->
            prefs[Keys.ENCRYPTED_REFRESH_TOKEN] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            prefs[Keys.REFRESH_TOKEN_IV] = Base64.encodeToString(iv, Base64.NO_WRAP)
        }
    }

    /**
     * Recupera el refresh token descifrado.
     * Devuelve null si no existe o si el descifrado falla (clave invalidada, datos corruptos).
     * En caso de fallo de descifrado, limpia los datos cifrados para forzar re-login.
     */
    suspend fun getRefreshToken(): String? {
        val prefs = dataStore.data.first()
        val encToken = prefs[Keys.ENCRYPTED_REFRESH_TOKEN] ?: return null
        val encIv = prefs[Keys.REFRESH_TOKEN_IV] ?: return null

        return try {
            val key = getOrCreateKey()
            val ciphertext = Base64.decode(encToken, Base64.NO_WRAP)
            val iv = Base64.decode(encIv, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            // La clave puede haber sido invalidada (root, factory reset, enrollment cambio).
            // Limpiar datos cifrados para forzar login explícito.
            clearRefreshToken()
            null
        }
    }

    /** Elimina el refresh token cifrado (logout). */
    suspend fun clearRefreshToken() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ENCRYPTED_REFRESH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN_IV)
        }
    }

    fun hasRefreshTokenFlow() = dataStore.data.map { it[Keys.ENCRYPTED_REFRESH_TOKEN] != null }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).also { it.load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }
}
