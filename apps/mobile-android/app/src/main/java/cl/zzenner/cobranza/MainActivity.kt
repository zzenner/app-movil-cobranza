package cl.zzenner.cobranza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.zzenner.cobranza.feature.auth.navigation.CobranzaNavGraph
import cl.zzenner.cobranza.feature.auth.ui.theme.CobranzaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CobranzaTheme {
                CobranzaNavGraph()
            }
        }
    }
}
