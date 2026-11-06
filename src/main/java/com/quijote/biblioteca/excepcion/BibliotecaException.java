package com.quijote.biblioteca.excepcion;

/** Base de las reglas de la casa que se quebrantan; el mensaje se muestra tal cual al usuario. */
public class BibliotecaException extends RuntimeException {

    public BibliotecaException(String mensaje) {
        super(mensaje);
    }
}
