package br.com.ifpe.medplus_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração global do OpenAPI para documentação da API.
 * Define informações da API, contato, licença e esquema de segurança JWT.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth"; // Nome do esquema de segurança

        return new OpenAPI()
                .info(new Info()
                        .title("MedPlus API")
                        .version("v1")
                        .description("API para o sistema de agendamento de consultas online da Clínica MedPlus.")
                        .termsOfService("http://medplus.com/termos")
                        .contact(new Contact()
                                .name("Suporte MedPlus")
                                .url("http://medplus.com/suporte")
                                .email("suporte@medplus.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                // Adiciona o esquema de segurança JWT Bearer
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP) // Tipo HTTP
                                        .scheme("bearer") // Esquema Bearer
                                        .bearerFormat("JWT") // Formato do token
                                        .description("Insira o token JWT no formato: Bearer {token}")));
    }
}

