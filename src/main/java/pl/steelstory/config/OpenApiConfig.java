package pl.steelstory.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "SteelStory", version = "1.0", description = "SteelStory API version 1.0"))
public class OpenApiConfig {
}
