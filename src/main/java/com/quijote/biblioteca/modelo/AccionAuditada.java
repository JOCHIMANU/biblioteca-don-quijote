package com.quijote.biblioteca.modelo;

/** Las acciones que quedan asentadas en el libro de auditoría. */
public enum AccionAuditada {
    PRESTAMO_ASENTADO("Préstamo asentado", false),
    DEVOLUCION("Devolución", false),
    PRESTAMO_ANULADO("Préstamo anulado", true),
    DEVOLUCION_DESHECHA("Devolución deshecha", true),
    LIBRO_REGISTRADO("Libro registrado", false),
    LIBRO_EDITADO("Libro corregido", false),
    LIBRO_BORRADO("Libro borrado", true),
    LECTOR_INSCRITO("Lector inscrito", false),
    LECTOR_EDITADO("Lector corregido", false),
    LECTOR_BORRADO("Lector borrado", true),
    CONFIGURACION("Configuración cambiada", true),
    PALOMA_DESPACHADA("Paloma despachada", false),
    RESPALDO("Respaldo descargado", false);

    private final String etiqueta;
    private final boolean delicada;

    AccionAuditada(String etiqueta, boolean delicada) {
        this.etiqueta = etiqueta;
        this.delicada = delicada;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Las que borran o cambian reglas: se resaltan con sello rojo. */
    public boolean isDelicada() {
        return delicada;
    }
}
