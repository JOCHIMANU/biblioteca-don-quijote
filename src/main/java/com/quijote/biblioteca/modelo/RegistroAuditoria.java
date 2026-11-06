package com.quijote.biblioteca.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Una línea del libro de auditoría: qué se hizo, cuándo y desde dónde. */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime momento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccionAuditada accion;

    @Column(nullable = false, length = 600)
    private String detalle;

    /** Dirección desde la que se hizo; vacía si la hizo el propio sistema (tareas programadas). */
    @Column(length = 60)
    private String origen;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(LocalDateTime momento, AccionAuditada accion, String detalle, String origen) {
        this.momento = momento;
        this.accion = accion;
        this.detalle = detalle;
        this.origen = origen;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getMomento() {
        return momento;
    }

    public AccionAuditada getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getOrigen() {
        return origen;
    }
}
