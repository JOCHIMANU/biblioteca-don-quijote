package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.LibroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sugerencias para los campos con búsqueda del formulario de préstamos (máximo 10 resultados). */
@RestController
public class BusquedaController {

    public record Sugerencia(Long id, String etiqueta, String detalle) {
    }

    private final LibroService libros;
    private final LectorService lectores;

    public BusquedaController(LibroService libros, LectorService lectores) {
        this.libros = libros;
        this.lectores = lectores;
    }

    @GetMapping("/api/libros/disponibles")
    public List<Sugerencia> librosDisponibles(@RequestParam(defaultValue = "") String q,
                                              @RequestParam(defaultValue = "") List<Long> excluir) {
        return libros.buscarDisponibles(q, excluir).stream()
                .map(v -> new Sugerencia(v.libro().getId(), etiquetaDeLibro(v.libro().getCodigo(), v.libro().getTitulo()),
                        v.libro().getAutor() + ", " + v.libro().getAnioImpresion()))
                .toList();
    }

    @GetMapping("/api/lectores/buscar")
    public List<Sugerencia> lectores(@RequestParam(defaultValue = "") String q) {
        var enPoder = lectores.librosEnPoder();
        return lectores.buscar(q).stream()
                .map(l -> {
                    long n = enPoder.getOrDefault(l.getId(), 0L);
                    String libros = n == 0 ? "" : " · lleva " + n + (n == 1 ? " libro" : " libros");
                    return new Sugerencia(l.getId(), l.getNombre(), "DNI " + l.getDni() + libros + " · " + l.getDomicilio());
                })
                .toList();
    }

    public static String etiquetaDeLibro(String codigo, String titulo) {
        return codigo + " · " + titulo;
    }
}
