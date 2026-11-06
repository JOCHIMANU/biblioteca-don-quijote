package com.quijote.biblioteca.controlador;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LibroForm {

    @NotBlank(message = "Falta el código: sin señal en el lomo, ¿cómo se hallará el libro?")
    @Size(max = 40, message = "El código no ha de pasar de cuarenta letras.")
    private String codigo;

    @NotBlank(message = "Falta el título de la obra.")
    @Size(max = 255, message = "El título es demasiado largo, que no es pergamino sin fin.")
    private String titulo;

    @NotBlank(message = "Falta el autor, o al menos el que se le atribuye.")
    @Size(max = 255, message = "El nombre del autor es demasiado largo.")
    private String autor;

    @NotNull(message = "Falta el año de impresión.")
    @Min(value = 1000, message = "El año de impresión ha de ser de 1000 en adelante.")
    @Max(value = 2100, message = "Ese año aún no ha llegado.")
    private Integer anioImpresion;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public Integer getAnioImpresion() {
        return anioImpresion;
    }

    public void setAnioImpresion(Integer anioImpresion) {
        this.anioImpresion = anioImpresion;
    }
}
