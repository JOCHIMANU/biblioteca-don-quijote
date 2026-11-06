package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.LibroYaPrestadoException;
import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.excepcion.ReglaDePrestamoException;
import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.repositorio.AvisoRepository;
import com.quijote.biblioteca.repositorio.LectorRepository;
import com.quijote.biblioteca.repositorio.LibroRepository;
import com.quijote.biblioteca.repositorio.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PrestamoService {

    private final PrestamoRepository prestamos;
    private final LibroRepository libros;
    private final LectorRepository lectores;
    private final AvisoRepository avisos;
    private final ConfiguracionService config;
    private final AuditoriaService auditoria;
    private final Clock reloj;

    public PrestamoService(PrestamoRepository prestamos, LibroRepository libros, LectorRepository lectores,
                           AvisoRepository avisos, ConfiguracionService config, AuditoriaService auditoria,
                           Clock reloj) {
        this.prestamos = prestamos;
        this.libros = libros;
        this.lectores = lectores;
        this.avisos = avisos;
        this.config = config;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    private static String resumen(Prestamo p) {
        return "«" + p.getLibro().getTitulo() + "» (" + p.getLibro().getCodigo() + ") — "
                + p.getLector().getNombre() + " (DNI " + p.getLector().getDni() + "), préstamo del " + p.getFechaPrestamo();
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    // ------------------------------------------------------------- reglas

    /** Plazo vigente hoy en la configuración (los préstamos nuevos lo heredan). */
    public int plazoDias() {
        return config.plazoDias();
    }

    public int multaPorDia() {
        return config.multaPorDia();
    }

    /** Plazo de un préstamo concreto: el que regía cuando se asentó. */
    public int plazoDe(Prestamo p) {
        return p.getPlazoDias() != null ? p.getPlazoDias() : config.plazoDias();
    }

    public int multaDe(Prestamo p) {
        return p.getMultaPorDia() != null ? p.getMultaPorDia() : config.multaPorDia();
    }

    public LocalDate fechaLimite(Prestamo p) {
        return p.getFechaPrestamo().plusDays(plazoDe(p));
    }

    /** Multa de un préstamo: max(0, días entre préstamo y devolución − plazo) × multa por día. */
    public int calcularMulta(Prestamo p, LocalDate fechaDevolucion) {
        return multa(p.getFechaPrestamo(), fechaDevolucion, plazoDe(p), multaDe(p));
    }

    /** Lo mismo con las reglas vigentes hoy (para consultas sin un préstamo a mano). */
    public int calcularMulta(LocalDate fechaPrestamo, LocalDate fechaDevolucion) {
        return multa(fechaPrestamo, fechaDevolucion, config.plazoDias(), config.multaPorDia());
    }

    private static int multa(LocalDate prestamo, LocalDate devolucion, int plazo, int porDia) {
        long diasDeRetraso = ChronoUnit.DAYS.between(prestamo, devolucion) - plazo;
        return (int) Math.max(0, diasDeRetraso) * porDia;
    }

    // ------------------------------------------------------- asentar y devolver

    @Transactional
    public Prestamo registrarPrestamo(Long libroId, Long lectorId, LocalDate fechaPrestamo) {
        return registrarPrestamos(List.of(libroId), lectorId, fechaPrestamo).get(0);
    }

    /**
     * Presta varios libros a un mismo lector. Cada libro queda como un préstamo propio (con su plazo y su
     * multa), pero la operación es de todo o nada: si uno solo anda ya en tierras ajenas, o el lector
     * rebasa el tope de la casa, no se asienta ninguno.
     */
    @Transactional
    public List<Prestamo> registrarPrestamos(List<Long> libroIds, Long lectorId, LocalDate fechaPrestamo) {
        List<Long> ids = libroIds == null ? List.<Long>of()
                : libroIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            throw new BibliotecaException("Escoged al menos un libro del anaquel.");
        }
        Lector lector = (lectorId == null ? Optional.<Lector>empty() : lectores.findById(lectorId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Escoged un lector del memorial: ese que nombráis no figura en él."));
        if (fechaPrestamo.isAfter(hoy())) {
            throw new BibliotecaException("No se puede asentar un préstamo que aún no ha ocurrido; esperad a su día.");
        }
        comprobarReglasDelLector(lector, ids.size());

        // Primero se comprueban todos los libros; solo si todos están libres se asienta alguno.
        List<Libro> aPrestar = new ArrayList<>();
        for (Long id : ids) {
            Libro libro = libros.buscarParaPrestar(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Escoged un libro del anaquel: ese que nombráis no se halla en la biblioteca."));
            prestamos.findByLibroIdAndFechaDevolucionIsNull(libro.getId()).ifPresent(abierto -> {
                throw new LibroYaPrestadoException(libro.getTitulo(), abierto.getLector().getNombre());
            });
            aPrestar.add(libro);
        }
        int plazo = config.plazoDias();
        int multa = config.multaPorDia();
        return aPrestar.stream()
                .map(l -> prestamos.save(new Prestamo(l, lector, fechaPrestamo, plazo, multa)))
                .peek(p -> auditoria.registrar(AccionAuditada.PRESTAMO_ASENTADO,
                        resumen(p) + "; plazo " + plazo + " días, multa " + multa + " sol(es) por día"))
                .toList();
    }

    private void comprobarReglasDelLector(Lector lector, int nuevos) {
        int tope = config.maxLibrosPorLector();
        if (tope > 0) {
            long enMano = prestamos.countByLectorIdAndFechaDevolucionIsNull(lector.getId());
            if (enMano + nuevos > tope) {
                long cabe = Math.max(0, tope - enMano);
                throw new ReglaDePrestamoException(lector.getNombre() + " ya lleva " + enMano
                        + (enMano == 1 ? " libro" : " libros") + " y el tope de la casa es de " + tope
                        + "; con " + nuevos + " más serían " + (enMano + nuevos) + ". "
                        + (cabe == 0 ? "Que devuelva alguno antes de llevarse otro."
                        : "Quitad algunos de la lista: solo caben " + cabe + "."));
            }
        }
        if (config.bloquearMorosos()) {
            long vencidos = prestamos.findByLectorIdAndFechaDevolucionIsNull(lector.getId()).stream()
                    .filter(p -> vista(p).moroso()).count();
            if (vencidos > 0) {
                throw new ReglaDePrestamoException(lector.getNombre() + " tiene " + vencidos
                        + (vencidos == 1 ? " libro vencido" : " libros vencidos")
                        + " sin devolver; la casa no presta a morosos hasta que se ponga al día.");
            }
        }
    }

    /** Asienta la devolución y devuelve la multa cobrada, en soles. */
    @Transactional
    public int devolver(Long prestamoId, LocalDate fechaDevolucion) {
        Prestamo prestamo = obtener(prestamoId);
        if (!prestamo.estaAbierto()) {
            throw new BibliotecaException("Este libro ya fue devuelto y asentado; no se devuelve dos veces.");
        }
        if (fechaDevolucion.isBefore(prestamo.getFechaPrestamo())) {
            throw new BibliotecaException("No se puede devolver un libro antes de haberlo prestado.");
        }
        if (fechaDevolucion.isAfter(hoy())) {
            throw new BibliotecaException("No se puede asentar una devolución que aún no ha ocurrido.");
        }
        int multa = calcularMulta(prestamo, fechaDevolucion);
        prestamo.devolver(fechaDevolucion, multa);
        auditoria.registrar(AccionAuditada.DEVOLUCION,
                resumen(prestamo) + "; devuelto el " + fechaDevolucion + "; multa: " + multa + " sol(es)");
        return multa;
    }

    // ------------------------------------------------------ corregir errores

    /** Anula un préstamo asentado por error (solo si el libro sigue fuera): borra el asiento y su aviso. */
    @Transactional
    public Prestamo anular(Long prestamoId) {
        Prestamo prestamo = obtener(prestamoId);
        if (!prestamo.estaAbierto()) {
            throw new BibliotecaException("Este préstamo ya se devolvió y se cobró; solo se puede deshacer su devolución.");
        }
        avisos.deleteByPrestamoId(prestamoId);
        prestamos.delete(prestamo);
        auditoria.registrar(AccionAuditada.PRESTAMO_ANULADO, resumen(prestamo));
        return prestamo;
    }

    /** Deshace una devolución asentada por error: el libro vuelve a estar prestado y la multa se borra. */
    @Transactional
    public Prestamo deshacerDevolucion(Long prestamoId) {
        Prestamo prestamo = obtener(prestamoId);
        if (prestamo.estaAbierto()) {
            throw new BibliotecaException("Este libro no consta como devuelto.");
        }
        libros.buscarParaPrestar(prestamo.getLibro().getId());
        prestamos.findByLibroIdAndFechaDevolucionIsNull(prestamo.getLibro().getId()).ifPresent(abierto -> {
            throw new BibliotecaException("«" + prestamo.getLibro().getTitulo() + "» ya se prestó de nuevo a "
                    + abierto.getLector().getNombre() + "; no se puede deshacer esta devolución.");
        });
        String antes = "devuelto el " + prestamo.getFechaDevolucion() + ", multa " + prestamo.getMulta() + " sol(es)";
        prestamo.deshacerDevolucion();
        auditoria.registrar(AccionAuditada.DEVOLUCION_DESHECHA, resumen(prestamo) + "; se borra: " + antes);
        return prestamo;
    }

    /** Al arrancar: los préstamos anteriores a esta versión heredan las reglas vigentes. */
    @Transactional
    public int completarReglasHistoricas() {
        List<Prestamo> sin = prestamos.findByPlazoDiasIsNull();
        sin.forEach(p -> p.fijarReglas(config.plazoDias(), config.multaPorDia()));
        return sin.size();
    }

    // ------------------------------------------------------------ consultas

    public Prestamo obtener(Long id) {
        return prestamos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No consta tal préstamo en el registro."));
    }

    /** Libros que están fuera, con días restantes o de atraso. */
    public List<PrestamoVista> abiertos() {
        return prestamos.findByFechaDevolucionIsNullOrderByFechaPrestamoAsc().stream().map(this::vista).toList();
    }

    /** Vencidos y no devueltos, del mayor al menor atraso. */
    public List<PrestamoVista> morosos() {
        return abiertos().stream()
                .filter(PrestamoVista::moroso)
                .sorted(Comparator.comparingLong(PrestamoVista::diasAtraso).reversed())
                .toList();
    }

    /** Préstamos devueltos entre dos fechas (ambas incluidas), por orden de devolución. */
    public List<PrestamoVista> devueltosEntre(LocalDate desde, LocalDate hasta) {
        return prestamos.findByFechaDevolucionBetweenOrderByFechaDevolucionAscIdAsc(desde, hasta).stream()
                .map(this::vista).toList();
    }

    public List<PrestamoVista> registroCompleto() {
        return prestamos.findAllByOrderByFechaPrestamoDescIdDesc().stream().map(this::vista).toList();
    }

    public List<PrestamoVista> historialDeLibro(Long libroId) {
        return prestamos.findByLibroIdOrderByFechaPrestamoDescIdDesc(libroId).stream().map(this::vista).toList();
    }

    public List<PrestamoVista> historialDeLector(Long lectorId) {
        return prestamos.findByLectorIdOrderByFechaPrestamoDescIdDesc(lectorId).stream().map(this::vista).toList();
    }

    public PrestamoVista vista(Prestamo p) {
        LocalDate limite = fechaLimite(p);
        LocalDate referencia = p.estaAbierto() ? hoy() : p.getFechaDevolucion();
        long diasHastaLimite = ChronoUnit.DAYS.between(referencia, limite);
        return new PrestamoVista(p, limite, diasHastaLimite, Math.max(0, -diasHastaLimite));
    }
}
