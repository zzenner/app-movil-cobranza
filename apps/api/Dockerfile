# ─── Etapa 1: caché de dependencias ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /build

# Copiar solo los archivos de declaración de dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Descargar dependencias sin compilar el proyecto
RUN ./mvnw dependency:go-offline -q

# ─── Etapa 2: compilación ────────────────────────────────────────────────────
FROM deps AS build
WORKDIR /build

# Copiar fuentes (sin tests — las pruebas se ejecutan por separado con mvnw verify)
COPY src/main/ src/main/

# Compilar y empaquetar saltando tests
RUN ./mvnw package -DskipTests -q && \
    mv target/cobranza-api-*.jar target/app.jar

# ─── Etapa 3: imagen runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# su-exec: abandona privilegios root antes de iniciar la JVM
RUN apk add --no-cache su-exec

# Usuario sin privilegios (UID/GID asignados automáticamente por Alpine)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copiar únicamente el JAR ejecutable
COPY --from=build /build/target/app.jar app.jar

# Copiar entrypoint que gestiona permisos de claves RSA y cede privilegios
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# /keys se monta como volumen read-only; las claves se copian en runtime por el entrypoint
RUN mkdir -p /keys

# Configuración de JVM para contenedor
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=America/Santiago"

# Puerto interno de Spring Boot
EXPOSE 8080

# Healthcheck vía Actuator readiness probe
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=8 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

# El entrypoint corre como root, copia las claves, cede a appuser y lanza la JVM
ENTRYPOINT ["/entrypoint.sh"]
