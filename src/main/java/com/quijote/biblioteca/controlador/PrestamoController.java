package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PrestamoController {

    private final PrestamoService prestamos;
    private final LibroService libros;
    private final LectorService lectores;

    public PrestamoController(PrestamoService prestamos, LibroService libros, LectorService lectores) {
        this.prestamos = prestamos;
        this.libros = libros;
        this.lectores = lectores;
    }

    /** "Libros en tierras ajenas": los que están fuera, y a petición el registro completo. */
    @GetMapping("/prestamos")
    public String fuera(@RequestParam(required = false) String todos, Model model) {
        model.addAttribute("abiertos", prestamos.abiertos());
        if (todos != null) {
            model.addAttribute("registro", prestamos.registroCompleto());
        }
        model.addAttribute("activo", 3);
        return "prestamos";
    }

    @GetMapping("/prestamos/nuevo")
    public String nuevo(@RequestParam(required = false) Long libroId,
                        @RequestParam(required = false) Long lectorId, Model model) {
        PrestamoForm form = new PrestamoForm();
        if (libroId != null) {
            form.getLibroIds().add(libroId);
        }
        form.setLectorId(lectorId);
        form.setFecha(prestamos.hoy());
        model.addAttribute("form", form);
        return formularioPrestamo(model, form);
    }

    @PostMapping("/prestamos")
    public String asentar(@Valid @ModelAttribute("form") PrestamoForm form, BindingResult resultado,
                          Model model, RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                List<Prestamo> asentados = prestamos.registrarPrestamos(form.getLibroIds(), form.getLectorId(), form.getFecha());
                String titulos = asentados.stream().map(p -> "«" + p.getLibro().getTitulo() + "»")
                        .collect(Collectors.joining(", "));
                String lector = asentados.get(0).getLector().getNombre();
                String plazo = prestamos.plazoDias() + " días tiene para devolver "
                        + (asentados.size() == 1 ? "el libro." : "cada uno.");
                flash.addFlashAttribute("ok", (asentados.size() == 1
                        ? "Préstamo asentado con sello de lacre: " + titulos + " a " + lector + ". "
                        : asentados.size() + " préstamos asentados con sello de lacre a " + lector + ": " + titulos + ". ")
                        + plazo);
                flash.addFlashAttribute("estampar", true);
                return "redirect:/prestamos";
            } catch (BibliotecaException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return formularioPrestamo(model, form);
    }

    /** Los campos con búsqueda necesitan los libros y el lector ya elegidos (si los hay) para volver a pintarlos. */
    private String formularioPrestamo(Model model, PrestamoForm form) {
        List<BusquedaController.Sugerencia> elegidos = new ArrayList<>();
        for (Long id : form.getLibroIds()) {
            try {
                var libro = libros.obtener(id);
                elegidos.add(new BusquedaController.Sugerencia(libro.getId(),
                        BusquedaController.etiquetaDeLibro(libro.getCodigo(), libro.getTitulo()), libro.getAutor()));
            } catch (RecursoNoEncontradoException e) {
                // un id inexistente simplemente no se pinta
            }
        }
        model.addAttribute("librosElegidos", elegidos);
        try {
            if (form.getLectorId() != null) {
                model.addAttribute("lectorEtiqueta", lectores.obtener(form.getLectorId()).getNombre());
            }
        } catch (RecursoNoEncontradoException e) {
            // idem
        }
        model.addAttribute("activo", 3);
        return "prestamo-nuevo";
    }

    /** Pantalla de confirmación: muestra la multa calculada antes de devolver. */
    @GetMapping("/prestamos/{id}/devolver")
    public String confirmar(@PathVariable Long id,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                            Model model) {
        Prestamo prestamo = prestamos.obtener(id);
        LocalDate dia = fecha != null ? fecha : prestamos.hoy();
        model.addAttribute("p", prestamo);
        model.addAttribute("fecha", dia);
        // las reglas son las vigentes cuando se asentó este préstamo, no las de hoy
        model.addAttribute("limite", prestamos.fechaLimite(prestamo));
        model.addAttribute("multa", prestamos.calcularMulta(prestamo, dia));
        model.addAttribute("plazo", prestamos.plazoDe(prestamo));
        model.addAttribute("multaPorDia", prestamos.multaDe(prestamo));
        model.addAttribute("activo", 3);
        return "devolver";
    }

    /** Anula un préstamo asentado por error (el libro sigue fuera): borra el asiento. */
    @PostMapping("/prestamos/{id}/anular")
    public String anular(@PathVariable Long id, @RequestParam(required = false) String volver, RedirectAttributes flash) {
        try {
            Prestamo p = prestamos.anular(id);
            flash.addFlashAttribute("ok", "Préstamo de «" + p.getLibro().getTitulo() + "» a " + p.getLector().getNombre()
                    + " anulado: el libro vuelve a figurar en el anaquel.");
        } catch (BibliotecaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + destinoSeguro(volver);
    }

    /** Deshace una devolución asentada por error: el libro vuelve a estar prestado y la multa se borra. */
    @PostMapping("/prestamos/{id}/deshacer-devolucion")
    public String deshacerDevolucion(@PathVariable Long id, @RequestParam(required = false) String volver,
                                     RedirectAttributes flash) {
        try {
            Prestamo p = prestamos.deshacerDevolucion(id);
            flash.addFlashAttribute("ok", "Devolución deshecha: «" + p.getLibro().getTitulo()
                    + "» vuelve a figurar en manos de " + p.getLector().getNombre() + " y la multa se borra.");
        } catch (BibliotecaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + destinoSeguro(volver);
    }

    /** Solo se vuelve a rutas internas de la propia app (nada de //otro-sitio ni http://…). */
    private static String destinoSeguro(String volver) {
        if (volver != null && volver.startsWith("/") && !volver.startsWith("//") && !volver.contains("\\")
                && !volver.contains("\r") && !volver.contains("\n")) {
            return volver;
        }
        return "/prestamos";
    }

    @PostMapping("/prestamos/{id}/devolver")
    public String devolver(@PathVariable Long id,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                           Model model, RedirectAttributes flash) {
        try {
            Prestamo prestamo = prestamos.obtener(id);
            int multa = prestamos.devolver(id, fecha);
            String texto = "«" + prestamo.getLibro().getTitulo() + "» vuelve al anaquel.";
            texto += multa > 0
                    ? " Multa cobrada: " + multa + (multa == 1 ? " sol." : " soles.")
                    : " Sin multa: llegó a tiempo.";
            flash.addFlashAttribute("ok", texto);
            return "redirect:/prestamos";
        } catch (BibliotecaException e) {
            model.addAttribute("error", e.getMessage());
            return confirmar(id, fecha, model);
        }
    }
}
