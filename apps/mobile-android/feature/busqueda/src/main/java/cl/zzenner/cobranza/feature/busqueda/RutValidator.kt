package cl.zzenner.cobranza.feature.busqueda

object RutValidator {

    fun esValido(numero: String, dv: String): Boolean {
        val numLimpio = numero.trim()
        val dvNorm = dv.trim().uppercase()

        if (numLimpio.isBlank() || dvNorm.isBlank()) return false
        if (!numLimpio.all { it.isDigit() }) return false
        if (numLimpio.length > 8) return false
        if (dvNorm != "K" && (dvNorm.length != 1 || !dvNorm[0].isDigit())) return false

        return dvNorm == calcularDv(numLimpio)
    }

    fun calcularDv(numero: String): String {
        var suma = 0
        var multiplicador = 2
        for (i in numero.indices.reversed()) {
            suma += numero[i].digitToInt() * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }
        return when (val resto = 11 - (suma % 11)) {
            11 -> "0"
            10 -> "K"
            else -> resto.toString()
        }
    }
}
