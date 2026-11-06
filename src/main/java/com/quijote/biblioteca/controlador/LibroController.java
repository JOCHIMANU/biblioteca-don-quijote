package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.CodigoDuplicadoException;
import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class LibroController {

    private final LibroService libros;
    private final PrestamoService prestamos;

    public LibroController(LibroService libros, PrestamoService prestamos) {
        this.libros = libros;
        this.prestamos = prestamos;
    }

    @GetMapping("/libros")
    public String anaquel(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("libros", libros.listar(q));
        model.addAttribute("q", q);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new LibroForm());
        }
        model.addAttribute("morosoLibros", idsDeLibrosMorosos());
        model.addAttribute("activo", 1);
        return "libros";
    }

    private Set<Long> idsDeLibrosMorosos() {
        return prestamos.morosos().stream().map(v -> v.prestamo().getLibro().getId()).collect(Collectors.toSet());
    }

    @PostMapping("/libros")
    public String registrar(@Valid @ModelAttribute("form") LibroForm form, BindingResult resultado,
                            Model model, RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                Libro libro = libros.registrar(form.getCodigo(), form.getTitulo(), form.getAutor(), form.getAnioImpresion());
                flash.addFlashAttribute("ok", "«" + libro.getTitulo() + "» queda asentado en el anaquel. ¡Que lo lean mil lectores!");
                return "redirect:/libros";
            } catch (CodigoDuplicadoException e) {
                resultado.rejectValue("codigo", "duplicado", e.getMessage());
            }
        }
        model.addAttribute("libros", libros.listar(null));
        model.addAttribute("q", "");
        model.addAttribute("morosoLibros", idsDeLibrosMorosos());
        model.addAttribute("activo", 1);
        model.addAttribute("abrirForm", true);
        return "libros";
    }

    @GetMapping("/libros/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Libro libro = libros.obtener(id);
        LibroForm form = new LibroForm();
        form.setCodigo(libro.getCodigo());
        form.setTitulo(libro.getTitulo());
        form.setAutor(libro.getAutor());
        form.setAnioImpresion(libro.getAnioImpresion());
        model.addAttribute("form", form);
        return vistaEdicion(model, libro);
    }

    @PostMapping("/libros/{id}/editar")
    public String guardarEdicion(@PathVariable Long id, @Valid @ModelAttribute("form") LibroForm form,
                                 BindingResult resultado, Model model, RedirectAttributes flash) {
        Libro libro = libros.obtener(id);
        if (!resultado.hasErrors()) {
            try {
                libros.actualizar(id, form.getCodigo(), form.getTitulo(), form.getAutor(), form.getAnioImpresion());
                flash.addFlashAttribute("ok", "Los datos de «" + form.getTitulo().trim() + "» quedan corregidos.");
                return "redirect:/libros";
            } catch (CodigoDuplicadoException e) {
                resultado.rejectValue("codigo", "duplicado", e.getMessage());
            }
        }
        return vistaEdicion(model, libro);
    }

    @PostMapping("/libros/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Libro borrado = libros.eliminar(id);
            flash.addFlashAttribute("ok", "«" + borrado.getTitulo() + "» queda borrado del anaquel.");
            return "redirect:/libros";
        } catch (BibliotecaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/libros/" + id;
        }
    }

    private String vistaEdicion(Model model, Libro libro) {
        model.addAttribute("libro", libro);
        model.addAttribute("activo", 1);
        return "libro-editar";
    }

    @GetMapping("/libros/{id}")
    public String historial(@PathVariable Long id, Model model) {
        Libro libro = libros.obtener(id);
        model.addAttribute("titulo", libro.getTitulo());
        model.addAttribute("detalle", libro.getAutor() + ", " + libro.getAnioImpresion() + " · lomo " + libro.getCodigo());
        model.addAttribute("porLibro", true);
        model.addAttribute("sujetoId", id);
        model.addAttribute("prestamos", prestamos.historialDeLibro(id));
        model.addAttribute("activo", 1);
        return "historial";
    }
}
