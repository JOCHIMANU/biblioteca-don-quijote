package com.quijote.biblioteca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelojConfig {

    /**
     * El reloj de la casa, en la zona horaria de la biblioteca (por defecto Lima) y no en la del servidor:
     * en la nube los servidores van en UTC, y de las 7 de la tarde a la medianoche de Perú «hoy» sería ya
     * mañana (préstamos con fecha del día siguiente y atrasos mal contados). En las pruebas se sustituye
     * por un reloj detenido.
     */
    @Bean
    public Clock reloj(@Value("${biblioteca.zona-horaria:America/Lima}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
