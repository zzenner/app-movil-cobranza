package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.carteras.api.CarteraConsultaApi;
import cl.zzenner.cobranza.importacion.dominio.*;
import cl.zzenner.cobranza.importacion.infraestructura.ErrorImportacionRepository;
import cl.zzenner.cobranza.importacion.infraestructura.ImportacionMensualRepository;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ImportacionService {

    private static final Logger log = LoggerFactory.getLogger(ImportacionService.class);

    private final ImportacionMensualRepository repository;
    private final ErrorImportacionRepository errorRepository;
    private final ArchivoImportacionStorage storage;
    private final CarteraConsultaApi carteraApi;
    private final UsuarioConsultaApi usuarioApi;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public ImportacionService(ImportacionMensualRepository repository,
                               ErrorImportacionRepository errorRepository,
                               ArchivoImportacionStorage storage,
                               CarteraConsultaApi carteraApi,
                               UsuarioConsultaApi usuarioApi,
                               ApplicationEventPublisher eventos,
                               Clock clock) {
        this.repository = repository;
        this.errorRepository = errorRepository;
        this.storage = storage;
        this.carteraApi = carteraApi;
        this.usuarioApi = usuarioApi;
        this.eventos = eventos;
        this.clock = clock;
    }

    public UUID recibirImportacion(UUID carteraId, String periodo, String sistemaOrigen,
                                    MultipartFile archivo, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);
        validarCartera(carteraId);
        validarPeriodo(periodo);
        validarArchivo(archivo);

        String nombreOriginal = sanitizarNombreArchivo(archivo.getOriginalFilename());
        String hashArchivo = calcularHash(archivo);

        // Verificar periodo anterior
        repository.findMaxPeriodoCompletado(carteraId, sistemaOrigen).ifPresent(maxPeriodo -> {
            if (periodo.compareTo(maxPeriodo) < 0) {
                throw new PeriodoAnteriorNoPermitidoException(periodo, maxPeriodo);
            }
        });

        // Idempotencia: mismo archivo ya importado completado
        repository.findByHashArchivoAndPeriodoAndCarteraIdAndSistemaOrigenAndEstado(
                hashArchivo, periodo, carteraId, sistemaOrigen, EstadoImportacion.COMPLETADA)
                .ifPresent(existente -> {
                    throw new ArchivoYaImportadoException(existente.getId());
                });

        // Guardar archivo primero
        UUID importacionId = UUID.randomUUID();
        String ruta;
        try (InputStream is = archivo.getInputStream()) {
            ruta = storage.guardar(importacionId, is, nombreOriginal);
        } catch (IOException e) {
            throw new UncheckedIOException("Error al guardar el archivo de importación", e);
        }

        // Crear registro en BD
        ImportacionMensual importacion;
        try {
            importacion = new ImportacionMensual(importacionId, carteraId, actorId, periodo,
                    sistemaOrigen, hashArchivo, nombreOriginal, ruta);
            repository.save(importacion);
        } catch (Exception e) {
            storage.eliminar(ruta);
            throw e;
        }

        // Publicar evento AFTER_COMMIT para trigger async
        eventos.publishEvent(new ValidarImportacionEvento(importacionId));

        log.info("[IMPORTACION] Recibida importacion={} periodo={} cartera={}", importacionId, periodo, carteraId);
        return importacionId;
    }

    public void confirmar(UUID importacionId, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);

        ImportacionMensual im = repository.findByIdConBloqueo(importacionId)
                .orElseThrow(() -> new ImportacionMensualNoEncontradaException(importacionId));

        if (!im.getEstado().permiteConfirmar()) {
            throw new EstadoInvalidoParaConfirmarException(importacionId, im.getEstado());
        }

        // Verificar si hay otra PROCESANDO para el mismo periodo/cartera
        List<ImportacionMensual> enProgreso = repository.findEnProgreso(
                im.getCarteraId(), im.getPeriodo(), im.getSistemaOrigen());
        if (!enProgreso.isEmpty()) {
            throw new ImportacionEnProgresoException(enProgreso.get(0).getId());
        }

        im.transicionarA(EstadoImportacion.PROCESANDO);
        repository.save(im);

        eventos.publishEvent(new ProcesarImportacionEvento(importacionId));
        log.info("[IMPORTACION] Confirmada importacion={}", importacionId);
    }

    @Transactional(readOnly = true)
    public ImportacionMensual obtener(UUID importacionId) {
        return repository.findById(importacionId)
                .orElseThrow(() -> new ImportacionMensualNoEncontradaException(importacionId));
    }

    @Transactional(readOnly = true)
    public Page<ImportacionMensual> listar(UUID carteraId, int pagina, int tamanio) {
        PageRequest page = PageRequest.of(pagina, tamanio,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        if (carteraId != null) {
            return repository.findByCarteraIdOrderByFechaCreacionDesc(carteraId, page);
        }
        return repository.findAllByOrderByFechaCreacionDesc(page);
    }

    @Transactional(readOnly = true)
    public Page<ErrorImportacion> listarErrores(UUID importacionId, int pagina, int tamanio) {
        repository.findById(importacionId)
                .orElseThrow(() -> new ImportacionMensualNoEncontradaException(importacionId));
        return errorRepository.findByImportacionIdOrderByNumeroFilaAscIdAsc(
                importacionId, PageRequest.of(pagina, tamanio));
    }

    private void validarActor(UUID actorId, Instant ahora) {
        var credenciales = usuarioApi.buscarCredencialesPorId(actorId)
                .orElseThrow(() -> new AccessDeniedException("Actor no encontrado"));
        if (!credenciales.isActivo() || credenciales.isBloqueado()) {
            throw new AccessDeniedException("El actor no tiene acceso activo");
        }
        if (credenciales.getBloqueadoHasta() != null && ahora.isBefore(credenciales.getBloqueadoHasta())) {
            throw new AccessDeniedException("El actor tiene un bloqueo temporal vigente");
        }
        if (!credenciales.getCodigosPermisos().contains("DATOS_IMPORTAR")) {
            throw new AccessDeniedException("El actor no tiene el permiso DATOS_IMPORTAR");
        }
    }

    private void validarCartera(UUID carteraId) {
        if (!carteraApi.existeActiva(carteraId)) {
            throw new CarteraNoActivaException(carteraId);
        }
    }

    private void validarPeriodo(String periodo) {
        if (periodo == null || !periodo.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException(
                    "El periodo debe tener formato YYYY-MM (ej: 2026-08)");
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV es requerido");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre != null && !nombre.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("El archivo debe tener extensión .csv");
        }
    }

    private String sanitizarNombreArchivo(String nombre) {
        if (nombre == null || nombre.isBlank()) return "importacion.csv";
        // Solo conservar el nombre del archivo sin path
        String base = nombre.replaceAll(".*[/\\\\]", "");
        // Remover caracteres peligrosos
        return base.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private String calcularHash(MultipartFile archivo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = archivo.getBytes();
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Error al calcular el hash del archivo", e);
        }
    }
}
