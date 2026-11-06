package com.quijote.biblioteca.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.time.LocalDate;

/** Aviso que la paloma mensajera lleva a Sancho Panza por un préstamo vencido. Uno por préstamo. */
@Entity
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "prestamo_id", unique = true)
    private Prestamo prestamo;

    @Column(nullable = false)
    private LocalDate creado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAviso estado = EstadoAviso.PENDIENTE;

    private LocalDate reclamadoEl;

    protected Aviso() {
    }

    public Aviso(Prestamo prestamo, LocalDate creado) {
        this.prestamo = prestamo;
        this.creado = creado;
    }

    public void reclamar(LocalDate fecha) {
        this.estado = EstadoAviso.RECLAMADO;
        this.reclamadoEl = fecha;
    }

    public Long getId() {
        return id;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public LocalDate getCreado() {
        return creado;
    }

    public EstadoAviso getEstado() {
        return estado;
    }

    public LocalDate getReclamadoEl() {
        return reclamadoEl;
    }
}
