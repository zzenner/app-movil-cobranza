package cl.zzenner.cobranza;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI openApiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("cobranza-api")
                        .version("0.1.0")
                        .description("API REST del sistema de cobranza en terreno."));
    }
}
