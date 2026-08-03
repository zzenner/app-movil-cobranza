package cl.zzenner.cobranza.autenticacion.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * Valida Origin (o Referer) en los endpoints web sensibles.
 * SameSite=Strict protege en navegadores, pero esta capa cubre clientes no-browser.
 */
@Component
@Order(1)
public class WebOriginValidationFilter extends OncePerRequestFilter {

    @Value("${app.web.allowed-origin:http://localhost:4200}")
    private String allowedOrigin;

    private static final Set<String> RUTAS_PROTEGIDAS = Set.of(
            "/api/v1/auth/web/refresh",
            "/api/v1/auth/web/logout"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        if (!RUTAS_PROTEGIDAS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String origin = obtenerOrigen(request);
        if (!allowedOrigin.equals(origin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/problem+json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Origen no permitido\"," +
                    "\"code\":\"ORIGEN_NO_PERMITIDO\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String obtenerOrigen(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return origin;
        }
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                URI uri = URI.create(referer);
                int port = uri.getPort();
                return uri.getScheme() + "://" + uri.getHost() + (port != -1 ? ":" + port : "");
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
