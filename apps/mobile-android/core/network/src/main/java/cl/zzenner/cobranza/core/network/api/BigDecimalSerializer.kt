package cl.zzenner.cobranza.core.network.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

/**
 * Serializador para [BigDecimal] que:
 * - Acepta tanto números JSON (como los que produce Jackson) como strings.
 * - Usa [BigDecimal.toPlainString] al serializar para evitar notación científica.
 * - Nunca usa Double en la conversión para preservar precisión exacta.
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val jsonDecoder = decoder as? JsonDecoder
        return if (jsonDecoder != null) {
            when (val element = jsonDecoder.decodeJsonElement()) {
                is JsonPrimitive -> {
                    try {
                        BigDecimal(element.content)
                    } catch (e: NumberFormatException) {
                        throw SerializationException(
                            "No se puede convertir '${element.content}' a BigDecimal",
                            e,
                        )
                    }
                }
                else -> throw SerializationException(
                    "Se esperaba un primitivo JSON para BigDecimal, se recibió ${element::class.simpleName}",
                )
            }
        } else {
            try {
                BigDecimal(decoder.decodeString())
            } catch (e: NumberFormatException) {
                throw SerializationException("BigDecimal inválido desde String", e)
            }
        }
    }
}
