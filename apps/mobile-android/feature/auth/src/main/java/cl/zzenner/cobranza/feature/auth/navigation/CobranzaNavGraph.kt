package cl.zzenner.cobranza.feature.auth.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.auth.ui.CheckScreen
import cl.zzenner.cobranza.feature.auth.ui.LoginScreen
import cl.zzenner.cobranza.feature.auth.ui.LoginViewModel

/**
 * Grafo de navegación del flujo de autenticación.
 *
 * Expone únicamente el flujo de auth (check + login) como un sub-grafo
 * con ruta raíz "auth". La navegación hacia otras features se delega
 * al callback [onLoginExitoso], que el host (:app) resuelve.
 *
 * Restricción: :feature:auth NO conoce a :feature:asignacion ni a ninguna
 * otra feature. Toda la coordinación es responsabilidad de :app.
 */
fun NavGraphBuilder.authNavGraph(
    navController: NavController,
    onLoginExitoso: () -> Unit,
) {
    navigation(startDestination = "check", route = "auth") {

        composable("check") {
            val viewModel: LoginViewModel = hiltViewModel()
            val authState by viewModel.authState.collectAsState()

            LaunchedEffect(Unit) { viewModel.verificarSesion() }

            LaunchedEffect(authState) {
                when (authState) {
                    is AuthState.NoAutenticado, is AuthState.Error ->
                        navController.navigate("login") {
                            popUpTo("check") { inclusive = true }
                        }
                    is AuthState.Autenticado -> onLoginExitoso()
                    else -> Unit
                }
            }

            CheckScreen()
        }

        composable("login") {
            val viewModel: LoginViewModel = hiltViewModel()
            val authState by viewModel.authState.collectAsState()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(authState) {
                if (authState is AuthState.Autenticado) onLoginExitoso()
            }

            LoginScreen(
                uiState = uiState,
                authState = authState,
                onUsuarioChanged = viewModel::onUsuarioChanged,
                onContrasenaChanged = viewModel::onContrasenaChanged,
                onLogin = viewModel::login,
            )
        }
    }
}
