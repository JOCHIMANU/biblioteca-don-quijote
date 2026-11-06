package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.DniDuplicadoException;
import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LectorController {

    private final LectorService lectores;
    private final PrestamoService prestamos;

    public LectorController(LectorService lectores, PrestamoService prestamos) {
        this.lectores = lectores;
        this.prestamos = prestamos;
    }

    @GetMapping("/lectores")
    public String memorial(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new LectorForm());
        }
        return vistaMemorial(model);
    }

    @PostMapping("/lectores")
    public String inscribir(@Valid @ModelAttribute("form") LectorForm form, BindingResult resultado,
                            Model model, RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                Lector lector = lectores.registrar(form.getNombre(), form.getDomicilio(), form.getCelular(), form.getDni());
                flash.addFlashAttribute("ok", lector.getNombre() + " queda inscrito en el memorial. ¡Bien venido sea a la biblioteca!");
                return "redirect:/lectores";
            } catch (DniDuplicadoException e) {
                resultado.rejectValue("dni", "duplicado", e.getMessage());
            }
        }
        model.addAttribute("abrirForm", true);
        return vistaMemorial(model);
    }

    private String vistaMemorial(Model model) {
        model.addAttribute("lectores", lectores.listar());
        model.addAttribute("enPoder", lectores.librosEnPoder());
        model.addAttribute("activo", 2);
        return "lectores";
    }

    @GetMapping("/lectores/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Lector lector = lectores.obtener(id);
        LectorForm form = new LectorForm();
        form.setNombre(lector.getNombre());
        form.setDomicilio(lector.getDomicilio());
        form.setCelular(lector.getCelular());
        form.setDni(lector.getDni());
        model.addAttribute("form", form);
        return vistaEdicion(model, lector);
    }

    @PostMapping("/lectores/{id}/editar")
    public String guardarEdicion(@PathVariable Long id, @Valid @ModelAttribute("form") LectorForm form,
                                 BindingResult resultado, Model model, RedirectAttributes flash) {
        Lector lector = lectores.obtener(id);
        if (!resultado.hasErrors()) {
            try {
                lectores.actualizar(id, form.getNombre(), form.getDomicilio(), form.getCelular(), form.getDni());
                flash.addFlashAttribute("ok", "Los datos de " + form.getNombre().trim() + " quedan corregidos en el memorial.");
                return "redirect:/lectores/" + id;
            } catch (DniDuplicadoException e) {
                resultado.rejectValue("dni", "duplicado", e.getMessage());
            }
        }
        return vistaEdicion(model, lector);
    }

    @PostMapping("/lectores/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Lector borrado = lectores.eliminar(id);
            flash.addFlashAttribute("ok", borrado.getNombre() + " queda borrado del memorial.");
            return "redirect:/lectores";
        } catch (BibliotecaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/lectores/" + id;
        }
    }

    private String vistaEdicion(Model model, Lector lector) {
        model.addAttribute("lector", lector);
        model.addAttribute("activo", 2);
        return "lector-editar";
    }

    @GetMapping("/lectores/{id}")
    public String historial(@PathVariable Long id, Model model) {
        Lector lector = lectores.obtener(id);
        model.addAttribute("titulo", lector.getNombre());
        model.addAttribute("detalle", lector.getDomicilio() + " · DNI " + lector.getDni() + " · cel. " + lector.getCelular());
        model.addAttribute("porLibro", false);
        model.addAttribute("sujetoId", id);
        model.addAttribute("prestamos", prestamos.historialDeLector(id));
        model.addAttribute("activo", 2);
        return "historial";
    }
}
