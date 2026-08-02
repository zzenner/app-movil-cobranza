package cl.zzenner.cobranza.feature.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.zzenner.cobranza.feature.auth.domain.AuthState

/**
 * Pantalla principal mínima de Fase 4A.
 *
 * Las funcionalidades de asignaciones, gestiones y sincronización
 * se agregan en Fases 4B y 4C. Los espacios futuros están deshabilitados
 * con indicación explícita al usuario.
 *
 * Extensión prevista: antes del logout (Fase 4C), verificar gestiones pendientes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authState: AuthState.Autenticado,
    onLogout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cobranza") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Cerrar sesión")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Bienvenido, ${authState.nombreUsuario}",
                style = MaterialTheme.typography.titleLarge,
            )

            HorizontalDivider()

            DisabledFeatureCard(
                titulo = "Mi asignación diaria",
                descripcion = "Disponible en Fase 4B",
            )

            DisabledFeatureCard(
                titulo = "Registrar gestión",
                descripcion = "Disponible en Fase 4B",
            )

            DisabledFeatureCard(
                titulo = "Sincronizar",
                descripcion = "Disponible en Fase 4B",
            )
        }
    }
}

@Composable
private fun DisabledFeatureCard(titulo: String, descripcion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}
