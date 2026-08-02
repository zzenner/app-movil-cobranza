package cl.zzenner.cobranza.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonaDaoTest {

    private lateinit var db: CobranzaDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CobranzaDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun insertarPersona(
        id: String = "p-1",
        rutNumero: String = "27000001",
        rutDv: String = "0",
    ) {
        db.personaDao().insertAll(listOf(PersonaEntity(id, rutNumero, rutDv, "Test Persona")))
    }

    @Test
    fun `buscarPorRut con rut sin puntos ni guion`() = runTest {
        insertarPersona()
        val result = db.personaDao().buscarPorRut("270000010")
        assertEquals(1, result.size)
    }

    @Test
    fun `buscarPorRut con dv K mayuscula normalizado`() = runTest {
        insertarPersona(rutNumero = "11111111", rutDv = "k")
        // search with uppercase K — normalizes via LOWER
        val result = db.personaDao().buscarPorRut("11111111k")
        assertEquals(1, result.size)
    }

    @Test
    fun `buscarPorRut parcial encuentra multiples`() = runTest {
        db.personaDao().insertAll(
            listOf(
                PersonaEntity("p-1", "27000001", "0", "Persona 1"),
                PersonaEntity("p-2", "27000002", "0", "Persona 2"),
                PersonaEntity("p-3", "18000000", "K", "Persona 3"),
            ),
        )
        val result = db.personaDao().buscarPorRut("2700000")
        assertEquals(2, result.size)
    }

    @Test
    fun `getPersonasDeAsignacion retorna relaciones completas`() = runTest {
        // Setup asignacion
        db.asignacionDiariaDao().insert(
            AsignacionDiariaEntity(
                id = "asig-1",
                ejecutivoId = "eje-1",
                fecha = "2026-08-02",
                estado = "PUBLICADA",
                fechaDescargaEpoch = 1000L,
            ),
        )
        insertarPersona()
        db.asignacionDiariaDao().insertAllCrossRefs(
            listOf(AsignacionPersonaCrossRef("asig-1", "p-1")),
        )
        db.personaDao().insertAllDirecciones(
            listOf(
                DireccionEntity(
                    personaId = "p-1", tipo = "DOMICILIO",
                    texto = "Calle 1", comuna = "Santiago",
                    ciudad = "Santiago", vigente = true,
                ),
            ),
        )

        val personas = db.personaDao().getPersonasDeAsignacion("asig-1").first()
        assertEquals(1, personas.size)
        val persona = personas.first()
        assertEquals("p-1", persona.persona.id)
        assertEquals(1, persona.direcciones.size)
        assertEquals("DOMICILIO", persona.direcciones.first().tipo)
    }

    @Test
    fun `buscarPorRut sin resultados retorna lista vacia`() = runTest {
        val result = db.personaDao().buscarPorRut("99999999")
        assertTrue(result.isEmpty())
    }
}
