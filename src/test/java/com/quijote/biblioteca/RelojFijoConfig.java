package com.quijote.biblioteca;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Detiene el reloj en un día fijo para que las pruebas no dependan de la fecha real. */
@TestConfiguration
public class RelojFijoConfig {

    public static final LocalDate HOY = LocalDate.of(2026, 10, 30);

    @Bean
    @Primary
    public Clock relojFijo() {
        return Clock.fixed(HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }
}
