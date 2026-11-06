package com.quijote.biblioteca.excepcion;

/** El préstamo choca con una regla de la casa configurable: tope de libros o lector moroso. */
public class ReglaDePrestamoException extends BibliotecaException {

    public ReglaDePrestamoException(String mensaje) {
        super(mensaje);
    }
}
