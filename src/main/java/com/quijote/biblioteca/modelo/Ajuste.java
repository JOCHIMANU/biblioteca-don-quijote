package com.quijote.biblioteca.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Un ajuste de la casa editable desde «VI. Configuración» (clave → valor). */
@Entity
public class Ajuste {

    @Id
    @Column(length = 60)
    private String clave;

    @Column(nullable = false)
    private String valor;

    protected Ajuste() {
    }

    public Ajuste(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() {
        return clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
