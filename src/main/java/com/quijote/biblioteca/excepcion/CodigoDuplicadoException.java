package com.quijote.biblioteca.excepcion;

public class CodigoDuplicadoException extends BibliotecaException {

    public CodigoDuplicadoException(String codigo) {
        super("El código «" + codigo + "» ya está escrito en otro lomo; cada libro ha de tener señal propia.");
    }
}
