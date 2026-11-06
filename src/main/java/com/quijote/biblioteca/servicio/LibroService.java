package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.CodigoDuplicadoException;
import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.repositorio.LibroRepository;
import com.quijote.biblioteca.repositorio.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class LibroService {

    private final LibroRepository libros;
    private final PrestamoRepository prestamos;
    private final AuditoriaService auditoria;

    public LibroService(LibroRepository libros, PrestamoRepository prestamos, AuditoriaService auditoria) {
        this.libros = libros;
        this.prestamos = prestamos;
        this.auditoria = auditoria;
    }

    private static String ficha(Libro l) {
        return "«" + l.getTitulo() + "» (" + l.getCodigo() + "), " + l.getAutor() + ", " + l.getAnioImpresion();
    }

    @Transactional
    public Libro registrar(String codigo, String titulo, String autor, int anioImpresion) {
        String codigoNormal = codigo.trim().toUpperCase(Locale.ROOT);
        if (libros.existsByCodigo(codigoNormal)) {
            throw new CodigoDuplicadoException(codigoNormal);
        }
        Libro nuevo = libros.save(new Libro(codigoNormal, titulo.trim(), autor.trim(), anioImpresion));
        auditoria.registrar(AccionAuditada.LIBRO_REGISTRADO, ficha(nuevo));
        return nuevo;
    }

    @Transactional
    public Libro actualizar(Long id, String codigo, String titulo, String autor, int anioImpresion) {
        Libro libro = obtener(id);
        String codigoNormal = codigo.trim().toUpperCase(Locale.ROOT);
        if (libros.existsByCodigoAndIdNot(codigoNormal, id)) {
            throw new CodigoDuplicadoException(codigoNormal);
        }
        String antes = ficha(libro);
        libro.actualizar(codigoNormal, titulo.trim(), autor.trim(), anioImpresion);
        auditoria.registrar(AccionAuditada.LIBRO_EDITADO, antes + "  →  " + ficha(libro));
        return libro;
    }

    /** Borra un libro solo si nunca se prestó: con historial se conserva, que de ahí salen las cuentas. */
    @Transactional
    public Libro eliminar(Long id) {
        Libro libro = obtener(id);
        if (prestamos.existsByLibroId(id)) {
            throw new BibliotecaException("«" + libro.getTitulo() + "» tiene préstamos en su historial y no se puede borrar; "
                    + "borrarlo falsearía las cuentas. Si hubo un error, anulad el préstamo equivocado.");
        }
        libros.delete(libro);
        auditoria.registrar(AccionAuditada.LIBRO_BORRADO, ficha(libro));
        return libro;
    }

    public Libro obtener(Long id) {
        return libros.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay tal libro en el anaquel."));
    }

    /** Todos los libros (o los que casen con la búsqueda), cada uno con su estado derivado. */
    public List<LibroVista> listar(String busqueda) {
        List<Libro> lista = (busqueda == null || busqueda.isBlank())
                ? libros.findAllByOrderByTituloAsc()
                : libros.buscar(patronDeBusqueda(busqueda));
        Map<Long, Prestamo> fuera = new HashMap<>();
        for (Prestamo p : prestamos.findByFechaDevolucionIsNullOrderByFechaPrestamoAsc()) {
            fuera.put(p.getLibro().getId(), p);
        }
        return lista.stream().map(l -> new LibroVista(l, fuera.get(l.getId()))).toList();
    }

    /** «amadís» → «%amadís%», con %, _ y \ escritos por el usuario tratados como letras normales. */
    static String patronDeBusqueda(String texto) {
        String limpio = texto.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + limpio + "%";
    }

    /** Hasta 10 libros libres (en el anaquel) que casen con título, autor o código. */
    public List<LibroVista> buscarDisponibles(String texto, Collection<Long> excluir) {
        return listar(texto).stream()
                .filter(v -> !v.prestado() && !excluir.contains(v.libro().getId()))
                .limit(10).toList();
    }
}
