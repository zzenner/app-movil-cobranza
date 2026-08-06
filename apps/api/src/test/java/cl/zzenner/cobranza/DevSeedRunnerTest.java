package cl.zzenner.cobranza;

import cl.zzenner.cobranza.usuarios.api.UsuarioSeedApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevSeedRunnerTest {

    @Mock UsuarioSeedApi usuarioSeedApi;

    DevSeedRunner runner;

    @BeforeEach
    void setUp() {
        runner = new DevSeedRunner(usuarioSeedApi);
    }

    private void configurarRunner(String username, String password, String email, String rol) {
        ReflectionTestUtils.setField(runner, "username", username);
        ReflectionTestUtils.setField(runner, "password", password);
        ReflectionTestUtils.setField(runner, "email", email);
        ReflectionTestUtils.setField(runner, "rol", rol);
    }

    @Test
    void crea_usuario_cuando_no_existe() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "admin@dev.cl", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste("dev.admin", "Secret1!", "admin@dev.cl", "TECNOLOGIA");
    }

    @Test
    void no_crea_usuario_si_ya_existe() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), isNull(), anyString()))
                .thenReturn(false);

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste(eq("dev.admin"), eq("Secret1!"), isNull(), eq("TECNOLOGIA"));
    }

    @Test
    void omite_seed_si_username_vacio() throws Exception {
        configurarRunner("", "Secret1!", "", "TECNOLOGIA");

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi, never()).crearSiNoExiste(any(), any(), any(), any());
    }

    @Test
    void omite_seed_si_password_vacio() throws Exception {
        configurarRunner("dev.admin", "", "", "TECNOLOGIA");

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi, never()).crearSiNoExiste(any(), any(), any(), any());
    }

    @Test
    void asigna_rol_TECNOLOGIA_por_defecto() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), isNull(), anyString()))
                .thenReturn(true);

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste(any(), any(), any(), eq("TECNOLOGIA"));
    }

    @Test
    void email_en_blanco_pasa_null_a_api() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "   ", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), isNull(), anyString()))
                .thenReturn(true);

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste(eq("dev.admin"), eq("Secret1!"), isNull(), eq("TECNOLOGIA"));
    }
}
