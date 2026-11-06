package com.quijote.biblioteca.controlador;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ConfiguracionForm {

    @NotNull(message = "Indicad el plazo en días.")
    @Min(value = 1, message = "El plazo ha de ser de al menos 1 día.")
    @Max(value = 365, message = "El plazo no puede pasar de 365 días.")
    private Integer plazoDias;

    @NotNull(message = "Indicad la multa por día (0 si no se cobra).")
    @Min(value = 0, message = "La multa no puede ser negativa.")
    @Max(value = 1000, message = "La multa por día no puede pasar de 1000 soles.")
    private Integer multaPorDia;

    @NotNull(message = "Indicad el tope de libros (0 si no hay límite).")
    @Min(value = 0, message = "El tope no puede ser negativo.")
    @Max(value = 100, message = "El tope no puede pasar de 100 libros.")
    private Integer maxLibrosPorLector;

    private boolean bloquearMorosos;

    public Integer getPlazoDias() {
        return plazoDias;
    }

    public void setPlazoDias(Integer plazoDias) {
        this.plazoDias = plazoDias;
    }

    public Integer getMultaPorDia() {
        return multaPorDia;
    }

    public void setMultaPorDia(Integer multaPorDia) {
        this.multaPorDia = multaPorDia;
    }

    public Integer getMaxLibrosPorLector() {
        return maxLibrosPorLector;
    }

    public void setMaxLibrosPorLector(Integer maxLibrosPorLector) {
        this.maxLibrosPorLector = maxLibrosPorLector;
    }

    public boolean isBloquearMorosos() {
        return bloquearMorosos;
    }

    public void setBloquearMorosos(boolean bloquearMorosos) {
        this.bloquearMorosos = bloquearMorosos;
    }
}
