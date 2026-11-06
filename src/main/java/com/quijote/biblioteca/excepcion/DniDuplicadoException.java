package com.quijote.biblioteca.excepcion;

public class DniDuplicadoException extends BibliotecaException {

    public DniDuplicadoException(String dni) {
        super("Ya figura en el memorial un lector con el DNI " + dni + "; no se inscribe dos veces a un mismo hidalgo.");
    }
}
