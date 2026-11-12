package com.steelstory.bootstrap.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = "sessionCookie", type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE, paramName = "SESSION",
        description = "HttpOnly session cookie. Obtain CSRF separately before unsafe requests.")
@OpenAPIDefinition(info = @Info(title = "Steelstory API", version = "v1"),
        servers = @Server(url = "/"))
class OpenApiConfiguration {
}
