package com.steelstory.bootstrap.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "Steelstory API", version = "v1"),
        servers = @Server(url = "/"))
class OpenApiConfiguration {
}
