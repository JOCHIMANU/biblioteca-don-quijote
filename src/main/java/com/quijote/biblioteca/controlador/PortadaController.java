package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.servicio.AvisoService;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.LibroVista;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.PrestamoService;
import com.quijote.biblioteca.servicio.PrestamoVista;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PortadaController {

    private final LibroService libros;
    private final LectorService lectores;
    private final PrestamoService prestamos;
    private final AvisoService avisos;

    public PortadaController(LibroService libros, LectorService lectores,
                             PrestamoService prestamos, AvisoService avisos) {
        this.libros = libros;
        this.lectores = lectores;
        this.prestamos = prestamos;
        this.avisos = avisos;
    }

    @GetMapping("/")
    public String portada(Model model) {
        avisos.generarAvisos(); // la paloma se prepara al abrir la casa
        List<LibroVista> todos = libros.listar(null);
        List<PrestamoVista> fuera = prestamos.abiertos();
        model.addAttribute("totalLibros", todos.size());
        model.addAttribute("enAnaquel", todos.stream().filter(v -> !v.prestado()).count());
        model.addAttribute("prestados", fuera.size());
        model.addAttribute("morosos", fuera.stream().filter(PrestamoVista::moroso).count());
        model.addAttribute("totalLectores", lectores.listar().size());
        model.addAttribute("avisosPendientes", avisos.contarPendientes());
        model.addAttribute("activo", 0);
        return "portada";
    }
}
