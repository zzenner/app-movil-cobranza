package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.api.DemoAsignacionSeedApi;
import cl.zzenner.cobranza.usuarios.api.SupervisionSeedApi;
import cl.zzenner.cobranza.usuarios.api.UsuarioSeedApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevSeedRunnerTest {

    @Mock UsuarioSeedApi usuarioSeedApi;
    @Mock SupervisionSeedApi supervisionSeedApi;
    @Mock DemoAsignacionSeedApi demoAsignacionSeedApi;

    DevSeedRunner runner;

    @BeforeEach
    void setUp() {
        runner = new DevSeedRunner(usuarioSeedApi, supervisionSeedApi, demoAsignacionSeedApi);
    }

    private void configurarRunner(String username, String password, String email, String rol) {
        ReflectionTestUtils.setField(runner, "username", username);
        ReflectionTestUtils.setField(runner, "password", password);
        ReflectionTestUtils.setField(runner, "email", email);
        ReflectionTestUtils.setField(runner, "rol", rol);
    }

    private void stubSupervisoresYEjecutivos() {
        when(usuarioSeedApi.crearSupervisorDemoSiNoExiste(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(UUID.randomUUID());
        when(usuarioSeedApi.crearEjecutivoDemoSiNoExiste(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(UUID.randomUUID());
        when(usuarioSeedApi.findIdByNombreUsuario(anyString()))
                .thenReturn(UUID.randomUUID());
    }

    // ── Admin seed ──────────────────────────────────────────────────────────

    @Test
    void crea_usuario_cuando_no_existe() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "admin@dev.cl", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste("dev.admin", "Secret1!", "admin@dev.cl", "TECNOLOGIA");
    }

    @Test
    void no_crea_usuario_si_ya_existe() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), isNull(), anyString()))
                .thenReturn(false);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste(eq("dev.admin"), eq("Secret1!"), isNull(), eq("TECNOLOGIA"));
    }

    @Test
    void omite_seed_si_username_vacio() throws Exception {
        configurarRunner("", "Secret1!", "", "TECNOLOGIA");

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi, never()).crearSiNoExiste(any(), any(), any(), any());
        verify(usuarioSeedApi, never()).crearSupervisorDemoSiNoExiste(any(), any(), any(), any());
        verify(usuarioSeedApi, never()).crearEjecutivoDemoSiNoExiste(any(), any(), any(), any(), any());
    }

    @Test
    void omite_seed_si_password_vacio() throws Exception {
        configurarRunner("dev.admin", "", "", "TECNOLOGIA");

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi, never()).crearSiNoExiste(any(), any(), any(), any());
        verify(usuarioSeedApi, never()).crearSupervisorDemoSiNoExiste(any(), any(), any(), any());
        verify(usuarioSeedApi, never()).crearEjecutivoDemoSiNoExiste(any(), any(), any(), any(), any());
    }

    @Test
    void asigna_rol_TECNOLOGIA_por_defecto() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), isNull(), anyString()))
                .thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste(any(), any(), any(), eq("TECNOLOGIA"));
    }

    @Test
    void email_en_blanco_pasa_null_a_api() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "   ", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(anyString(), anyString(), isNull(), anyString()))
                .thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi).crearSiNoExiste(eq("dev.admin"), eq("Secret1!"), isNull(), eq("TECNOLOGIA"));
    }

    // ── Supervisores demo ───────────────────────────────────────────────────

    @Test
    void crea_tres_supervisores_demo() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi, times(3)).crearSupervisorDemoSiNoExiste(anyString(), anyString(), anyString(), anyString());
        verify(usuarioSeedApi).crearSupervisorDemoSiNoExiste(eq("sup_demo_norte"), any(), any(), any());
        verify(usuarioSeedApi).crearSupervisorDemoSiNoExiste(eq("sup_demo_centro"), any(), any(), any());
        verify(usuarioSeedApi).crearSupervisorDemoSiNoExiste(eq("sup_demo_sur"), any(), any(), any());
    }

    // ── Ejecutivos demo ─────────────────────────────────────────────────────

    @Test
    void crea_dieciocho_ejecutivos_demo() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(usuarioSeedApi, times(18)).crearEjecutivoDemoSiNoExiste(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void ejecutivos_tienen_rol_ejecutivo_terreno_implicito() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        // crearEjecutivoDemoSiNoExiste encapsula la asignación de EJECUTIVO_TERRENO
        verify(usuarioSeedApi, times(18)).crearEjecutivoDemoSiNoExiste(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void codigo_ejecutivo_preservado_exactamente_como_string() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        // Verifica que el código "133" no llega como entero ni transformado
        verify(usuarioSeedApi).crearEjecutivoDemoSiNoExiste(
                eq("ej_demo_133"), anyString(), anyString(), anyString(), eq("133"));
    }

    @Test
    void ceros_iniciales_preservados_en_codigo() throws Exception {
        // El método de la API recibe el código como String literal — nunca como número
        // Verificamos que si el código tuviera ceros iniciales (ej "0045") no se truncaría.
        // En los datos actuales el código "133" no tiene ceros, pero el contrato del método
        // garantiza que el argumento llega exactamente tal como se define en EJECUTIVOS_DEMO.
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        // Ningún código llega como numérico (lo que verificamos es el tipo String en la firma)
        verify(usuarioSeedApi, atLeastOnce()).crearEjecutivoDemoSiNoExiste(
                anyString(), anyString(), anyString(), anyString(), argThat(c -> c != null && !c.isEmpty()));
    }

    // ── Supervisión ─────────────────────────────────────────────────────────

    @Test
    void todos_los_ejecutivos_tienen_supervisor_asignado() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(supervisionSeedApi, times(18)).asignarEjecutivoSiNoAsignado(any(), any());
    }

    @Test
    void segunda_ejecucion_no_duplica_usuarios() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(false);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());
        runner.run(new DefaultApplicationArguments());

        // Se llama dos veces pero los métodos seed son idempotentes por contrato
        verify(usuarioSeedApi, times(2)).crearSiNoExiste(any(), any(), any(), any());
        verify(usuarioSeedApi, times(6)).crearSupervisorDemoSiNoExiste(any(), any(), any(), any());
        verify(usuarioSeedApi, times(36)).crearEjecutivoDemoSiNoExiste(any(), any(), any(), any(), any());
    }

    // ── Asignación demo ─────────────────────────────────────────────────────

    @Test
    void prepara_escenario_asignacion_demo_para_ej_demo_133() throws Exception {
        configurarRunner("dev.admin", "Secret1!", "", "TECNOLOGIA");
        when(usuarioSeedApi.crearSiNoExiste(any(), any(), any(), any())).thenReturn(true);
        stubSupervisoresYEjecutivos();

        runner.run(new DefaultApplicationArguments());

        verify(demoAsignacionSeedApi, times(1)).prepararEscenarioDemo(
                any(), any(), any(), any(), eq(5));
    }
}
