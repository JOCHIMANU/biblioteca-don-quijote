package com.quijote.biblioteca.controlador;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class LoginController {

    /**
     * Pantalla de entrada. Los avisos (error, bloqueado, salió, expirada) los pinta la plantilla según el parámetro.
     * El token CSRF se pide aquí, antes de dibujar la página: así la sesión se crea con la respuesta aún sin
     * enviar. Si se dejara para el formulario, el encabezado ya habría llenado el búfer de salida y el servidor
     * no podría crear la cookie de sesión («Cannot create a session after the response has been committed»).
     */
    @GetMapping("/login")
    public String entrada(Principal usuario, CsrfToken csrf) {
        if (usuario != null) {
            return "redirect:/";
        }
        csrf.getToken();
        return "login";
    }
}
