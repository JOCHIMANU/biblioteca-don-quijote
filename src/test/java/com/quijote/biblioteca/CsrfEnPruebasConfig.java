package com.quijote.biblioteca;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Las pruebas de pantallas envían siempre un token CSRF válido, como lo hace el navegador con los formularios. */
@TestConfiguration
public class CsrfEnPruebasConfig {

    @Bean
    MockMvcBuilderCustomizer csrfPorDefecto() {
        return constructor -> constructor.defaultRequest(MockMvcRequestBuilders.get("/").with(csrf()));
    }
}
