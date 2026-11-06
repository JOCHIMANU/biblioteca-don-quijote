package com.quijote.biblioteca.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Corta los intentos de entrada de una dirección bloqueada, antes de siquiera mirar la clave. */
public class FiltroBloqueoDeAcceso extends OncePerRequestFilter {

    private final LimiteDeIntentos limite;

    public FiltroBloqueoDeAcceso(LimiteDeIntentos limite) {
        this.limite = limite;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        boolean esIntentoDeEntrada = "POST".equals(peticion.getMethod())
                && (peticion.getContextPath() + "/login").equals(peticion.getRequestURI());
        if (esIntentoDeEntrada && limite.bloqueada(peticion.getRemoteAddr())) {
            respuesta.sendRedirect(peticion.getContextPath() + "/login?bloqueado");
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }
}
