package dev.haypacomer.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

  static final String BEARER = "bearer";
  static final String DEVICE_KEY = "deviceKey";

  @Bean
  OpenAPI haypacomerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("HayPaComer API")
                .version("v1")
                .description(
                    "Smart fridge API: households, live inventory in grams, cold chain, guided"
                        + " cooking, planning, and notifications. Errors follow RFC 7807."))
        .servers(List.of(new Server().url("https://api.haypacomer.dev")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(
                    DEVICE_KEY,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Device-Key")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }
}
