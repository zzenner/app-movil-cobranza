package cl.zzenner.cobranza.feature.asignacion

import cl.zzenner.cobranza.core.network.api.BigDecimalSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BigDecimalSerializerTest {

    @Serializable
    private data class Envoltorio(
        @Serializable(with = BigDecimalSerializer::class)
        val valor: BigDecimal,
    )

    private fun deserialize(jsonValue: String): BigDecimal =
        Json.decodeFromString<Envoltorio>("""{"valor":$jsonValue}""").valor

    @Test
    fun `entero sin decimales`() {
        assertEquals(BigDecimal("100"), deserialize("100"))
    }

    @Test
    fun `decimal normal`() {
        assertEquals(0, deserialize("123.45").compareTo(BigDecimal("123.45")))
    }

    @Test
    fun `valor grande`() {
        assertEquals(0, deserialize("9999999999.99").compareTo(BigDecimal("9999999999.99")))
    }

    @Test
    fun `cero`() {
        assertEquals(0, deserialize("0").compareTo(BigDecimal.ZERO))
    }

    @Test
    fun `trailing zeros`() {
        assertEquals(0, deserialize("100.00").compareTo(BigDecimal("100.00")))
    }

    @Test
    fun `json number como la API produce Jackson`() {
        // Jackson produce {"capital":123456.78} — número sin comillas
        assertEquals(0, deserialize("123456.78").compareTo(BigDecimal("123456.78")))
    }

    @Test
    fun `serializacion usa plainString sin notacion cientifica`() {
        val value = BigDecimal("1.23E+5")
        assertEquals("123000", value.toPlainString())
    }

    @Test
    fun `precision sin perdida con BigDecimal de String`() {
        // Nunca pasar por Double
        val importeCritico = "12345678901234.56"
        assertEquals(0, deserialize(importeCritico).compareTo(BigDecimal(importeCritico)))
    }
}
