package cl.zzenner.cobranza.feature.auth.domain

/**
 * Estados del flujo de autenticación.
 *
 * - [Verificando]: la app acaba de iniciarse y está comprobando si hay sesión activa.
 * - [NoAutenticado]: no hay sesión válida; mostrar pantalla de login.
 * - [Autenticando]: login en progreso.
 * - [Autenticado]: sesión activa, el usuario puede operar la app.
 * - [Error]: error recuperable o no recuperable que se muestra en UI.
 *
 * Extensión prevista (Fase 4C): un estado [BloqueadoPorGestionesPendientes] para
 * impedir el logout mientras existan gestiones sin sincronizar. No implementado en 4A.
 */
sealed class AuthState {
    data object Verificando : AuthState()
    data object NoAutenticado : AuthState()
    data object Autenticando : AuthState()
    data class Autenticado(val nombreUsuario: String) : AuthState()
    data class Error(val tipo: ErrorTipo, val mensaje: String = "") : AuthState()
}

enum class ErrorTipo {
    CREDENCIALES_INCORRECTAS,
    DISPOSITIVO_CONFLICTO,
    DISPOSITIVO_REVOCADO,
    SESION_EXPIRADA,
    SIN_CONEXION,
    TIMEOUT,
    ERROR_SERVIDOR,
    ERROR_DESCONOCIDO,
}
