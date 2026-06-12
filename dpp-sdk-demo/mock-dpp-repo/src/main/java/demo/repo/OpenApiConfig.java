package demo.repo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DPP Mock Repository API",
        version = "1.0.0",
        description = "Local demo/mock DPP repository service for interactive testing. Not a production repository service."
))
class OpenApiConfig {
}
