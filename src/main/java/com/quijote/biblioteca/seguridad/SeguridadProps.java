package com.quijote.biblioteca.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos de acceso a la biblioteca. La clave NUNCA se escribe en el código ni en el repositorio:
 * llega por la variable de entorno BIBLIOTECA_CLAVE (o por config/application.properties, que git ignora).
 */
@ConfigurationProperties(prefix = "biblioteca.seguridad")
public class SeguridadProps {

    private String usuario = "admin";
    /** Vacía = se genera una clave al azar en cada arranque y se escribe una vez en el registro. */
    private String clave = "";
    /** Fallos seguidos desde una misma dirección que bloquean los nuevos intentos. */
    private int maxIntentos = 5;
    /** Minutos que dura el conteo (y por tanto el bloqueo) desde el primer fallo. */
    private int minutosBloqueo = 15;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public int getMaxIntentos() {
        return maxIntentos;
    }

    public void setMaxIntentos(int maxIntentos) {
        this.maxIntentos = maxIntentos;
    }

    public int getMinutosBloqueo() {
        return minutosBloqueo;
    }

    public void setMinutosBloqueo(int minutosBloqueo) {
        this.minutosBloqueo = minutosBloqueo;
    }
}
