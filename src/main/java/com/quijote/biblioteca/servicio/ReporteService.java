package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.modelo.Prestamo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reportes descargables en CSV. Van con punto y coma, UTF-8 con marca BOM y saltos de línea de Windows,
 * que es lo que Excel en español abre bien con un doble clic.
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    public record Reporte(String nombreArchivo, byte[] contenido) {
    }

    private static final String SEP = ";";
    private static final String SALTO = "\r\n";
    private static final Pattern PARECE_TELEFONO = Pattern.compile("^\\+?[0-9 ]+$");

    private final LibroService libros;
    private final LectorService lectores;
    private final PrestamoService prestamos;

    public ReporteService(LibroService libros, LectorService lectores, PrestamoService prestamos) {
        this.libros = libros;
        this.lectores = lectores;
        this.prestamos = prestamos;
    }

    /** Genera el reporte {@code tipo}: libros, lectores, prestamos, morosos o multas (con desde y hasta). */
    public Reporte generar(String tipo, LocalDate desde, LocalDate hasta) {
        LocalDate hoy = prestamos.hoy();
        return switch (tipo) {
            case "libros" -> reporteDeLibros(hoy);
            case "lectores" -> reporteDeLectores(hoy);
            case "prestamos" -> reporteDePrestamos(hoy);
            case "morosos" -> reporteDeMorosos(hoy);
            case "multas" -> reporteDeMultas(hoy, desde, hasta);
            default -> throw new RecursoNoEncontradoException("No existe ese reporte en la biblioteca.");
        };
    }

    private Reporte reporteDeLibros(LocalDate hoy) {
        StringBuilder sb = fila(new StringBuilder(), "Código", "Título", "Autor", "Año de impresión", "Estado",
                "En manos de", "Debe volver el");
        for (LibroVista v : libros.listar(null)) {
            var p = v.prestamoActual();
            fila(sb, v.libro().getCodigo(), v.libro().getTitulo(), v.libro().getAutor(), v.libro().getAnioImpresion(),
                    v.estado(), p == null ? "" : p.getLector().getNombre(),
                    p == null ? "" : prestamos.fechaLimite(p));
        }
        return archivo("libros", hoy, sb);
    }

    private Reporte reporteDeLectores(LocalDate hoy) {
        StringBuilder sb = fila(new StringBuilder(), "Nombre", "DNI", "Celular", "Domicilio", "Libros en su poder");
        Map<Long, Long> enPoder = lectores.librosEnPoder();
        lectores.listar().forEach(l -> fila(sb, l.getNombre(), l.getDni(), l.getCelular(), l.getDomicilio(),
                enPoder.getOrDefault(l.getId(), 0L)));
        return archivo("lectores", hoy, sb);
    }

    private Reporte reporteDePrestamos(LocalDate hoy) {
        StringBuilder sb = fila(new StringBuilder(), "Código", "Libro", "Lector", "DNI", "Prestado el",
                "Plazo (días)", "Debía volver el", "Devuelto el", "Días de atraso", "Multa (soles)", "Estado");
        for (PrestamoVista v : prestamos.registroCompleto()) {
            Prestamo p = v.prestamo();
            String estado = !p.estaAbierto() ? "Devuelto" : (v.moroso() ? "Moroso" : "Prestado");
            fila(sb, p.getLibro().getCodigo(), p.getLibro().getTitulo(), p.getLector().getNombre(),
                    p.getLector().getDni(), p.getFechaPrestamo(), prestamos.plazoDe(p), v.fechaLimite(),
                    p.getFechaDevolucion() == null ? "" : p.getFechaDevolucion(), v.diasAtraso(),
                    p.getMulta() == null ? "" : p.getMulta(), estado);
        }
        return archivo("prestamos", hoy, sb);
    }

    private Reporte reporteDeMorosos(LocalDate hoy) {
        StringBuilder sb = fila(new StringBuilder(), "Libro", "Código", "Lector", "Celular", "Domicilio",
                "Prestado el", "Debía volver el", "Días de atraso");
        for (PrestamoVista v : prestamos.morosos()) {
            Prestamo p = v.prestamo();
            fila(sb, p.getLibro().getTitulo(), p.getLibro().getCodigo(), p.getLector().getNombre(),
                    p.getLector().getCelular(), p.getLector().getDomicilio(), p.getFechaPrestamo(),
                    v.fechaLimite(), v.diasAtraso());
        }
        return archivo("morosos", hoy, sb);
    }

    /** Multas cobradas: préstamos con multa devueltos entre {@code desde} y {@code hasta}, con su total. */
    private Reporte reporteDeMultas(LocalDate hoy, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : hoy;
        LocalDate inicio = desde != null ? desde : fin.withDayOfMonth(1);
        if (inicio.isAfter(fin)) {
            throw new BibliotecaException("La fecha «desde» no puede ser posterior a la fecha «hasta».");
        }
        StringBuilder sb = fila(new StringBuilder(), "Devuelto el", "Libro", "Lector", "DNI", "Prestado el",
                "Días de atraso", "Multa (soles)");
        int total = 0;
        int cuantas = 0;
        for (PrestamoVista v : prestamos.devueltosEntre(inicio, fin)) {
            Prestamo p = v.prestamo();
            if (p.getMulta() == null || p.getMulta() == 0) {
                continue;
            }
            fila(sb, p.getFechaDevolucion(), p.getLibro().getTitulo(), p.getLector().getNombre(),
                    p.getLector().getDni(), p.getFechaPrestamo(), v.diasAtraso(), p.getMulta());
            total += p.getMulta();
            cuantas++;
        }
        fila(sb, "TOTAL (" + cuantas + (cuantas == 1 ? " multa" : " multas") + ", del " + inicio + " al " + fin + ")",
                "", "", "", "", "", total);
        return new Reporte("biblioteca-multas-" + inicio + "_a_" + fin + ".csv", aBytes(sb));
    }

    // ------------------------------------------------------------------ CSV

    private static Reporte archivo(String nombre, LocalDate hoy, StringBuilder sb) {
        return new Reporte("biblioteca-" + nombre + "-" + hoy + ".csv", aBytes(sb));
    }

    private static byte[] aBytes(StringBuilder sb) {
        // BOM al inicio para que Excel reconozca el UTF-8 y respete las tildes
        return ("﻿" + sb).getBytes(StandardCharsets.UTF_8);
    }

    private static StringBuilder fila(StringBuilder sb, Object... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(SEP);
            }
            sb.append(celda(valores[i]));
        }
        return sb.append(SALTO);
    }

    /**
     * Escapa una celda: entre comillas si lleva separador, comillas o saltos de línea, y con una comilla simple
     * delante si empieza como una fórmula (=, +, -, @), para que una hoja de cálculo nunca la ejecute.
     */
    static String celda(Object valor) {
        String s = valor == null ? "" : valor.toString();
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0 && !PARECE_TELEFONO.matcher(s).matches()) {
            s = "'" + s;
        }
        if (s.contains(SEP) || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
