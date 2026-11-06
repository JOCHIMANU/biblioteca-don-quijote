package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.servicio.AvisoService;
import com.quijote.biblioteca.servicio.PrestamoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.time.LocalDate;

/** Datos comunes a todas las páginas (contador de palomas, fecha de hoy, usuario) y página de error. */
@ControllerAdvice
public class AvisosAdvice {

    private final AvisoService avisos;
    private final PrestamoService prestamos;

    public AvisosAdvice(AvisoService avisos, PrestamoService prestamos) {
        this.avisos = avisos;
        this.prestamos = prestamos;
    }

    /** Quién ha entrado (nulo en la pantalla de entrada): la cabecera solo muestra el índice a quien entró. */
    @ModelAttribute("usuario")
    public String usuario(Principal principal) {
        return principal != null ? principal.getName() : null;
    }

    @ModelAttribute("avisosPendientes")
    public long avisosPendientes(Principal principal) {
        return principal != null ? avisos.contarPendientes() : 0;
    }

    @ModelAttribute("plazoDias")
    public int plazoDias() {
        return prestamos.plazoDias();
    }

    @ModelAttribute("multaPorDia")
    public int multaPorDia() {
        return prestamos.multaPorDia();
    }

    @ModelAttribute("hoy")
    public LocalDate hoy() {
        return prestamos.hoy();
    }

    /** Cualquier regla de la casa que no se atrapó antes (p. ej. un reporte con fechas al revés). */
    @ExceptionHandler(BibliotecaException.class)
    public String reglaQuebrantada(BibliotecaException e, Model model, HttpServletResponse respuesta) {
        respuesta.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("mensaje", e.getMessage());
        model.addAttribute("activo", 0);
        return "error";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String noEncontrado(RecursoNoEncontradoException e, Model model, HttpServletResponse respuesta) {
        respuesta.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("mensaje", e.getMessage());
        model.addAttribute("activo", 0);
        return "error";
    }
}
