package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.modelo.Prestamo;

/** Un libro con su estado derivado: "En el anaquel" o "Prestado" (si hay préstamo abierto). */
public record LibroVista(Libro libro, Prestamo prestamoActual) {

    public static final String EN_ANAQUEL = "En el anaquel";
    public static final String PRESTADO = "Prestado";

    public boolean prestado() {
        return prestamoActual != null;
    }

    public String estado() {
        return prestado() ? PRESTADO : EN_ANAQUEL;
    }
}
