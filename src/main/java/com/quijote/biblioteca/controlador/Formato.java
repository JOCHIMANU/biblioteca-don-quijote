package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.servicio.PrestamoVista;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Ayudas de presentación para las plantillas (se usan como ${@fmt.fecha(...)}). */
@Component("fmt")
public class Formato {

    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic"};

    public String fecha(LocalDate fecha) {
        if (fecha == null) {
            return "—";
        }
        return fecha.getDayOfMonth() + " " + MESES[fecha.getMonthValue() - 1] + ". " + fecha.getYear();
    }

    public String fechaHora(java.time.LocalDateTime momento) {
        if (momento == null) {
            return "—";
        }
        return fecha(momento.toLocalDate()) + ", " + String.format("%02d:%02d", momento.getHour(), momento.getMinute());
    }

    public String dias(long n) {
        return n == 1 ? "1 día" : n + " días";
    }

    public String soles(int n) {
        return n == 1 ? "1 sol" : n + " soles";
    }

    /** Texto del plazo de un préstamo: días restantes o de atraso. */
    public String plazo(PrestamoVista v) {
        if (!v.prestamo().estaAbierto()) {
            return v.diasAtraso() > 0 ? "devuelto con " + dias(v.diasAtraso()) + " de atraso" : "devuelto a tiempo";
        }
        if (v.diasAtraso() > 0) {
            return dias(v.diasAtraso()) + " de atraso";
        }
        if (v.diasRestantes() == 0) {
            return "vence hoy";
        }
        return "faltan " + dias(v.diasRestantes());
    }
}
