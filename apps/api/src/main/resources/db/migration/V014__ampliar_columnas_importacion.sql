-- tipo_operacion en el archivo real tiene hasta 51 caracteres; aumentar a 200 para dejar margen.
ALTER TABLE cobranza.operaciones ALTER COLUMN tipo_operacion TYPE VARCHAR(200);
