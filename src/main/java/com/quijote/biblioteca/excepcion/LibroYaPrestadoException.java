package com.quijote.biblioteca.excepcion;

public class LibroYaPrestadoException extends BibliotecaException {

    public LibroYaPrestadoException(String titulo, String lector) {
        super("«" + titulo + "» anda ya en tierras ajenas, en manos de " + lector
                + "; no puede prestarse dos veces hasta que vuelva al anaquel.");
    }
}
