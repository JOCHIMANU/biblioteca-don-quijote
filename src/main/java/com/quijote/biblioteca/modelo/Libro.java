package com.quijote.biblioteca.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Un libro de la biblioteca (un solo ejemplar). El estado se deriva de los préstamos abiertos. */
@Entity
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Señal que Don Quijote escribe en el lomo; única y siempre en mayúsculas. */
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String autor;

    @Column(nullable = false)
    private int anioImpresion;

    protected Libro() {
    }

    public Libro(String codigo, String titulo, String autor, int anioImpresion) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.autor = autor;
        this.anioImpresion = anioImpresion;
    }

    public void actualizar(String codigo, String titulo, String autor, int anioImpresion) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.autor = autor;
        this.anioImpresion = anioImpresion;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public int getAnioImpresion() {
        return anioImpresion;
    }
}
