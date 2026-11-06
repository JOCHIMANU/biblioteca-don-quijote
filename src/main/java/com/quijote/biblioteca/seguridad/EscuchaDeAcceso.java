package com.quijote.biblioteca.seguridad;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/** Anota los intentos de entrada, buenos y malos, para el freno de {@link LimiteDeIntentos}. */
@Component
public class EscuchaDeAcceso {

    private final LimiteDeIntentos limite;

    public EscuchaDeAcceso(LimiteDeIntentos limite) {
        this.limite = limite;
    }

    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        limite.fallo(direccion(evento.getAuthentication()));
    }

    @EventListener
    public void alEntrar(AuthenticationSuccessEvent evento) {
        limite.exito(direccion(evento.getAuthentication()));
    }

    private static String direccion(Authentication autenticacion) {
        return autenticacion.getDetails() instanceof WebAuthenticationDetails detalles
                ? detalles.getRemoteAddress() : "desconocida";
    }
}
