package com.quijote.biblioteca.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Lector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    /** Lugar donde mora. */
    @Column(nullable = false)
    private String domicilio;

    @Column(nullable = false, length = 20)
    private String celular;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    protected Lector() {
    }

    public Lector(String nombre, String domicilio, String celular, String dni) {
        this.nombre = nombre;
        this.domicilio = domicilio;
        this.celular = celular;
        this.dni = dni;
    }

    public void actualizar(String nombre, String domicilio, String celular, String dni) {
        this.nombre = nombre;
        this.domicilio = domicilio;
        this.celular = celular;
        this.dni = dni;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public String getCelular() {
        return celular;
    }

    public String getDni() {
        return dni;
    }
}
