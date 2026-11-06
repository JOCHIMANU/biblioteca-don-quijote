package com.quijote.biblioteca.excepcion;

/** La consulta del DNI no pudo completarse; {@code noEncontrado} distingue "no existe" de "no hay servicio". */
public class ConsultaDniException extends BibliotecaException {

    private final boolean noEncontrado;

    public ConsultaDniException(String mensaje, boolean noEncontrado) {
        super(mensaje);
        this.noEncontrado = noEncontrado;
    }

    public boolean esNoEncontrado() {
        return noEncontrado;
    }
}
