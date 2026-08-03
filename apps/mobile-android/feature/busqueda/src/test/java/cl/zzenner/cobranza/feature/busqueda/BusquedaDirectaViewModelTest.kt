package cl.zzenner.cobranza.feature.busqueda

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BusquedaDirectaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: BusquedaDirectaRepository = mockk()
    private lateinit var viewModel: BusquedaDirectaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = BusquedaDirectaViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial es el esperado`() {
        val state = viewModel.state.value
        assertTrue(state.rutNumero.isEmpty())
        assertTrue(state.rutDv.isEmpty())
        assertFalse(state.buscando)
        assertFalse(state.rutInvalido)
        assertFalse(state.noEncontrada)
        assertNull(state.personaEncontradaId)
        assertNull(state.error)
    }

    @Test
    fun `onRutNumeroChanged filtra no digitos`() {
        viewModel.onRutNumeroChanged("12A34B")
        assertEquals("1234", viewModel.state.value.rutNumero)
    }

    @Test
    fun `onRutNumeroChanged limita a 8 digitos`() {
        viewModel.onRutNumeroChanged("123456789")
        assertEquals("12345678", viewModel.state.value.rutNumero)
    }

    @Test
    fun `onRutDvChanged normaliza a mayuscula`() {
        viewModel.onRutDvChanged("k")
        assertEquals("K", viewModel.state.value.rutDv)
    }

    @Test
    fun `buscar con rut invalido establece rutInvalido true`() = runTest {
        coEvery { repository.buscar(any(), any()) } returns ResultadoBusqueda.RutInvalido
        viewModel.onRutNumeroChanged("12345678")
        viewModel.onRutDvChanged("9")
        viewModel.buscar()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.rutInvalido)
        assertFalse(viewModel.state.value.buscando)
    }

    @Test
    fun `buscar con no encontrada establece noEncontrada true`() = runTest {
        coEvery { repository.buscar(any(), any()) } returns ResultadoBusqueda.NoEncontrada
        viewModel.onRutNumeroChanged("12345678")
        viewModel.onRutDvChanged("5")
        viewModel.buscar()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.noEncontrada)
        assertNull(viewModel.state.value.personaEncontradaId)
    }

    @Test
    fun `buscar encontrada establece personaEncontradaId`() = runTest {
        val personaId = "550e8400-e29b-41d4-a716-446655440000"
        coEvery { repository.buscar(any(), any()) } returns ResultadoBusqueda.Encontrada(personaId)
        viewModel.onRutNumeroChanged("12345678")
        viewModel.onRutDvChanged("5")
        viewModel.buscar()
        advanceUntilIdle()
        assertEquals(personaId, viewModel.state.value.personaEncontradaId)
        assertFalse(viewModel.state.value.buscando)
    }

    @Test
    fun `limpiarNavegacion borra personaEncontradaId`() = runTest {
        val personaId = "550e8400-e29b-41d4-a716-446655440000"
        coEvery { repository.buscar(any(), any()) } returns ResultadoBusqueda.Encontrada(personaId)
        viewModel.onRutNumeroChanged("12345678")
        viewModel.onRutDvChanged("5")
        viewModel.buscar()
        advanceUntilIdle()
        viewModel.limpiarNavegacion()
        assertNull(viewModel.state.value.personaEncontradaId)
    }

    @Test
    fun `buscar sin conexion establece mensaje de error`() = runTest {
        coEvery { repository.buscar(any(), any()) } returns ResultadoBusqueda.SinConexion
        viewModel.onRutNumeroChanged("12345678")
        viewModel.onRutDvChanged("5")
        viewModel.buscar()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.error?.contains("conexión") == true)
        assertFalse(viewModel.state.value.buscando)
    }
}
