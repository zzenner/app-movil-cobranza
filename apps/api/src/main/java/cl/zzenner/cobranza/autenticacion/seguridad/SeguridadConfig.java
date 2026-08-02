package cl.zzenner.cobranza.autenticacion.seguridad;

import cl.zzenner.cobranza.autenticacion.aplicacion.PropiedadesJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(PropiedadesJwt.class)
class SeguridadConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                              JwtAuthenticationConverter jwtAuthConverter,
                                              ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health/**",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthConverter))
                .authenticationEntryPoint((req, res, ex) ->
                    escribirProblemDetail(req, res, 401, "NO_AUTENTICADO", "No autenticado", objectMapper))
                .accessDeniedHandler((req, res, ex) ->
                    escribirProblemDetail(req, res, 403, "ACCESO_DENEGADO", "Acceso denegado", objectMapper))
            );
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extraerAuthorities);
        return converter;
    }

    private List<GrantedAuthority> extraerAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .forEach(authorities::add);
        }
        List<String> permisos = jwt.getClaimAsStringList("permisos");
        if (permisos != null) {
            permisos.stream()
                .map(p -> new SimpleGrantedAuthority("PERM_" + p))
                .forEach(authorities::add);
        }
        return authorities;
    }

    private static void escribirProblemDetail(HttpServletRequest request, HttpServletResponse response,
                                               int status, String code, String title,
                                               ObjectMapper mapper) throws IOException {
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(title);
        detail.setProperty("code", code);
        detail.setProperty("timestamp", Instant.now().toString());
        detail.setProperty("path", request.getRequestURI());
        response.setStatus(status);
        response.setContentType("application/problem+json;charset=UTF-8");
        response.getWriter().write(mapper.writeValueAsString(detail));
    }
}
