package com.quijote.biblioteca;

import com.quijote.biblioteca.config.RelojConfig;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** «Hoy» es el de Lima aunque el servidor esté en UTC (el caso de la nube). */
class RelojTest {

    @Test
    void elRelojDeLaCasaVaEnLaZonaDeLaBibliotecaYNoEnLaDelServidor() {
        Clock reloj = new RelojConfig().reloj("America/Lima");

        assertThat(reloj.getZone()).isEqualTo(ZoneId.of("America/Lima"));
        // 9 de la noche del 30 de octubre en Lima (UTC-5) = 02:00 UTC del 31: para la biblioteca sigue siendo 30
        Instant nocheEnLima = Instant.parse("2026-10-31T02:00:00Z");
        assertThat(LocalDate.now(Clock.fixed(nocheEnLima, reloj.getZone()))).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(LocalDate.now(Clock.fixed(nocheEnLima, ZoneId.of("UTC")))).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void laZonaSePuedeCambiarPorConfiguracion() {
        assertThat(new RelojConfig().reloj("America/Bogota").getZone()).isEqualTo(ZoneId.of("America/Bogota"));
    }
}
