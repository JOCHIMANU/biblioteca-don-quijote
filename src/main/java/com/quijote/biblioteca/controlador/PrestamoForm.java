package com.quijote.biblioteca.controlador;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PrestamoForm {

    @NotEmpty(message = "Escoged al menos un libro que sale del anaquel.")
    private List<Long> libroIds = new ArrayList<>();

    @NotNull(message = "Escoged el lector que se los lleva.")
    private Long lectorId;

    @NotNull(message = "Indicad la fecha del préstamo.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    public List<Long> getLibroIds() {
        return libroIds;
    }

    public void setLibroIds(List<Long> libroIds) {
        this.libroIds = libroIds;
    }

    public Long getLectorId() {
        return lectorId;
    }

    public void setLectorId(Long lectorId) {
        this.lectorId = lectorId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
