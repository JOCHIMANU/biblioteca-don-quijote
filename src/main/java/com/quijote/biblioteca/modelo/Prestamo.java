package com.quijote.biblioteca.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;

@Entity
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "libro_id")
    private Libro libro;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lector_id")
    private Lector lector;

    @Column(nullable = false)
    private LocalDate fechaPrestamo;

    /** Nula mientras el libro siga fuera. */
    private LocalDate fechaDevolucion;

    /** Multa en soles, fijada al devolver. */
    private Integer multa;

    /**
     * Plazo y multa vigentes cuando se asentó el préstamo: cambiar la configuración después
     * no altera los préstamos en curso. Nulos en datos antiguos (se completan al arrancar).
     */
    private Integer plazoDias;
    private Integer multaPorDia;

    protected Prestamo() {
    }

    public Prestamo(Libro libro, Lector lector, LocalDate fechaPrestamo, int plazoDias, int multaPorDia) {
        this.libro = libro;
        this.lector = lector;
        this.fechaPrestamo = fechaPrestamo;
        this.plazoDias = plazoDias;
        this.multaPorDia = multaPorDia;
    }

    public void devolver(LocalDate fecha, int multa) {
        this.fechaDevolucion = fecha;
        this.multa = multa;
    }

    /** Deshace una devolución asentada por error: el libro vuelve a figurar como prestado. */
    public void deshacerDevolucion() {
        this.fechaDevolucion = null;
        this.multa = null;
    }

    public void fijarReglas(int plazoDias, int multaPorDia) {
        this.plazoDias = plazoDias;
        this.multaPorDia = multaPorDia;
    }

    public Integer getPlazoDias() {
        return plazoDias;
    }

    public Integer getMultaPorDia() {
        return multaPorDia;
    }

    public boolean estaAbierto() {
        return fechaDevolucion == null;
    }

    public Long getId() {
        return id;
    }

    public Libro getLibro() {
        return libro;
    }

    public Lector getLector() {
        return lector;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    public Integer getMulta() {
        return multa;
    }
}
