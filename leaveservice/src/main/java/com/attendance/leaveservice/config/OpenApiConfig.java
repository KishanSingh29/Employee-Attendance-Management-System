package com.attendance.leaveservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "Bearer Auth";

    @Bean
    public OpenAPI leaveServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leave Service API")
                        .description("PAID/SICK/UNPAID leave management. Weekend-aware day calculation. "
                                + "Leave balance tracking (PL:12/year, SL:6/year). Salary deduction calculation "
                                + "for unpaid leaves. HR approval workflow.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
