package com.quijote.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos para consultar el DNI en Decolecta. El token nunca se escribe en el código: viene del entorno. */
@ConfigurationProperties(prefix = "decolecta")
public class DecolectaProps {

    private String url = "https://api.decolecta.com/v1/reniec/dni";
    private String token = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean tieneToken() {
        return token != null && !token.isBlank();
    }
}
