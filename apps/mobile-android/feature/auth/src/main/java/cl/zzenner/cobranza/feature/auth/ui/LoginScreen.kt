package cl.zzenner.cobranza.feature.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.auth.domain.ErrorTipo

/**
 * Pantalla de login.
 *
 * - Valida usuario y contraseña en el ViewModel.
 * - Muestra mensajes de error diferenciados según el tipo (credenciales, dispositivo, red).
 * - No almacena ni registra las credenciales ingresadas.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    authState: AuthState,
    onUsuarioChanged: (String) -> Unit,
    onContrasenaChanged: (String) -> Unit,
    onLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var mostrarContrasena by remember { mutableStateOf(false) }
    val cargando = authState is AuthState.Autenticando

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Cobranza",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        OutlinedTextField(
            value = uiState.usuario,
            onValueChange = onUsuarioChanged,
            label = { Text("Usuario") },
            singleLine = true,
            isError = uiState.errorUsuario != null,
            supportingText = uiState.errorUsuario?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.contrasena,
            onValueChange = onContrasenaChanged,
            label = { Text("Contraseña") },
            singleLine = true,
            isError = uiState.errorContrasena != null,
            supportingText = uiState.errorContrasena?.let { { Text(it) } },
            visualTransformation = if (mostrarContrasena) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus(); onLogin() },
            ),
            trailingIcon = {
                IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                    Icon(
                        imageVector = if (mostrarContrasena) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (mostrarContrasena) "Ocultar contraseña"
                        else "Mostrar contraseña",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        if (authState is AuthState.Error) {
            Text(
                text = mensajeDeError(authState.tipo),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Button(
            onClick = onLogin,
            enabled = !cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Ingresar")
            }
        }
    }
}

private fun mensajeDeError(tipo: ErrorTipo): String = when (tipo) {
    ErrorTipo.CREDENCIALES_INCORRECTAS -> "Usuario o contraseña incorrectos."
    ErrorTipo.DISPOSITIVO_CONFLICTO ->
        "Este dispositivo no puede asociarse a esta cuenta. Contacte a soporte."
    ErrorTipo.DISPOSITIVO_REVOCADO ->
        "El acceso desde este dispositivo ha sido revocado. Contacte a soporte."
    ErrorTipo.SIN_CONEXION -> "Sin conexión a Internet. Verifique su red."
    ErrorTipo.TIMEOUT -> "La conexión tardó demasiado. Intente nuevamente."
    ErrorTipo.ERROR_SERVIDOR -> "Error en el servidor. Intente más tarde."
    ErrorTipo.SESION_EXPIRADA -> "La sesión expiró. Ingrese nuevamente."
    ErrorTipo.ERROR_DESCONOCIDO -> "Error inesperado. Intente nuevamente."
}
