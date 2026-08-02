package cl.zzenner.cobranza.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.auth.ui.CheckScreen
import cl.zzenner.cobranza.feature.auth.ui.HomeScreen
import cl.zzenner.cobranza.feature.auth.ui.LoginScreen
import cl.zzenner.cobranza.feature.auth.ui.LoginViewModel

private object Routes {
    const val CHECK = "check"
    const val LOGIN = "login"
    const val HOME = "home"
}

/**
 * Grafo de navegación principal.
 * La pantalla de inicio es siempre [CHECK]; desde allí el estado de autenticación
 * determina si navegar a [LOGIN] o [HOME].
 */
@Composable
fun CobranzaNavGraph(
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Verificando -> Unit
            is AuthState.NoAutenticado, is AuthState.Error ->
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.CHECK) { inclusive = true }
                }
            is AuthState.Autenticando -> Unit
            is AuthState.Autenticado ->
                navController.navigate(Routes.HOME) {
                    popUpTo(0) { inclusive = true }
                }
        }
    }

    NavHost(navController = navController, startDestination = Routes.CHECK) {

        composable(Routes.CHECK) {
            LaunchedEffect(Unit) { viewModel.verificarSesion() }
            CheckScreen()
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = uiState,
                authState = authState,
                onUsuarioChanged = viewModel::onUsuarioChanged,
                onContrasenaChanged = viewModel::onContrasenaChanged,
                onLogin = viewModel::login,
            )
        }

        composable(Routes.HOME) {
            val autenticado = authState as? AuthState.Autenticado
                ?: AuthState.Autenticado("Usuario")
            HomeScreen(
                authState = autenticado,
                onLogout = viewModel::logout,
            )
        }
    }
}
