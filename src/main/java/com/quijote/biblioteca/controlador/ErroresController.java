package com.quijote.biblioteca.controlador;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/** Página de error con el tono de la casa (reemplaza la de Spring, que habla en inglés). */
@Controller
public class ErroresController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest peticion, HttpServletResponse respuesta, Model model) {
        Object codigo = peticion.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int estado = codigo instanceof Integer i ? i : 500;
        respuesta.setStatus(estado);
        model.addAttribute("mensaje", switch (estado) {
            case 400 -> "Esa petición no es de recibo: algún dato vino mal escrito.";
            case 401, 403 -> "No tenéis permiso para eso, o el formulario caducó: recargad la página e intentadlo de nuevo.";
            case 404 -> "Esta hoja del libro no existe o se la llevó el viento de los molinos.";
            case 405 -> "Esa acción no se hace por ese camino.";
            default -> "Algo falló en la casa, y no es culpa vuestra. Probad de nuevo en un rato.";
        });
        model.addAttribute("activo", 0);
        return "error";
    }
}
