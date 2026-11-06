package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.modelo.Prestamo;

import java.time.LocalDate;

/**
 * Un préstamo con sus plazos ya calculados.
 * Si sigue abierto, el atraso se cuenta hasta hoy; si ya volvió, hasta el día de la devolución.
 */
public record PrestamoVista(Prestamo prestamo, LocalDate fechaLimite, long diasRestantes, long diasAtraso) {

    /** Moroso: vencido y todavía sin devolver. */
    public boolean moroso() {
        return prestamo.estaAbierto() && diasAtraso > 0;
    }
}
