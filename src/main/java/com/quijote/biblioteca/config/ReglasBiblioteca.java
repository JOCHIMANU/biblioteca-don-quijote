package com.quijote.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Valores iniciales de las reglas de la casa (prefijo "biblioteca" en application.properties).
 * Una vez que se guardan en «VI. Configuración», mandan los valores de la base de datos.
 */
@ConfigurationProperties(prefix = "biblioteca")
public class ReglasBiblioteca {

    private int plazoDias = 7;
    private int multaPorDia = 1;
    /** 0 = sin límite. */
    private int maxLibrosPorLector = 0;
    private boolean bloquearMorosos = false;

    public int getPlazoDias() {
        return plazoDias;
    }

    public void setPlazoDias(int plazoDias) {
        this.plazoDias = plazoDias;
    }

    public int getMultaPorDia() {
        return multaPorDia;
    }

    public void setMultaPorDia(int multaPorDia) {
        this.multaPorDia = multaPorDia;
    }

    public int getMaxLibrosPorLector() {
        return maxLibrosPorLector;
    }

    public void setMaxLibrosPorLector(int maxLibrosPorLector) {
        this.maxLibrosPorLector = maxLibrosPorLector;
    }

    public boolean isBloquearMorosos() {
        return bloquearMorosos;
    }

    public void setBloquearMorosos(boolean bloquearMorosos) {
        this.bloquearMorosos = bloquearMorosos;
    }
}
