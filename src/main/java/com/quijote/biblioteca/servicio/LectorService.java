package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.DniDuplicadoException;
import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.repositorio.LectorRepository;
import com.quijote.biblioteca.repositorio.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class LectorService {

    private final LectorRepository lectores;
    private final PrestamoRepository prestamos;
    private final AuditoriaService auditoria;

    public LectorService(LectorRepository lectores, PrestamoRepository prestamos, AuditoriaService auditoria) {
        this.lectores = lectores;
        this.prestamos = prestamos;
        this.auditoria = auditoria;
    }

    private static String ficha(Lector l) {
        return l.getNombre() + " (DNI " + l.getDni() + "), cel. " + l.getCelular() + ", " + l.getDomicilio();
    }

    @Transactional
    public Lector registrar(String nombre, String domicilio, String celular, String dni) {
        String dniLimpio = dni.trim();
        if (lectores.existsByDni(dniLimpio)) {
            throw new DniDuplicadoException(dniLimpio);
        }
        Lector nuevo = lectores.save(new Lector(nombre.trim(), domicilio.trim(), celular.trim(), dniLimpio));
        auditoria.registrar(AccionAuditada.LECTOR_INSCRITO, ficha(nuevo));
        return nuevo;
    }

    @Transactional
    public Lector actualizar(Long id, String nombre, String domicilio, String celular, String dni) {
        Lector lector = obtener(id);
        String dniLimpio = dni.trim();
        if (lectores.existsByDniAndIdNot(dniLimpio, id)) {
            throw new DniDuplicadoException(dniLimpio);
        }
        String antes = ficha(lector);
        lector.actualizar(nombre.trim(), domicilio.trim(), celular.trim(), dniLimpio);
        auditoria.registrar(AccionAuditada.LECTOR_EDITADO, antes + "  →  " + ficha(lector));
        return lector;
    }

    /** Borra un lector solo si nunca tomó un libro: con historial se conserva, que de ahí salen las cuentas. */
    @Transactional
    public Lector eliminar(Long id) {
        Lector lector = obtener(id);
        if (prestamos.existsByLectorId(id)) {
            throw new BibliotecaException(lector.getNombre() + " tiene préstamos en su historial y no se puede borrar; "
                    + "borrarlo falsearía las cuentas. Si hubo un error, anulad el préstamo equivocado.");
        }
        lectores.delete(lector);
        auditoria.registrar(AccionAuditada.LECTOR_BORRADO, ficha(lector));
        return lector;
    }

    public Lector obtener(Long id) {
        return lectores.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No figura tal lector en el memorial."));
    }

    /** Hasta 10 lectores cuyo nombre o DNI contengan el texto (todos los primeros si viene vacío). */
    public List<Lector> buscar(String texto) {
        String q = texto == null ? "" : texto.trim();
        return lectores.findTop10ByNombreContainingIgnoreCaseOrDniContainingOrderByNombreAsc(q, q);
    }

    public List<Lector> listar() {
        return lectores.findAllByOrderByNombreAsc();
    }

    /** Cuántos libros tiene cada lector en su poder ahora mismo (id de lector → cantidad). */
    public Map<Long, Long> librosEnPoder() {
        Map<Long, Long> cuenta = new HashMap<>();
        for (Prestamo p : prestamos.findByFechaDevolucionIsNullOrderByFechaPrestamoAsc()) {
            cuenta.merge(p.getLector().getId(), 1L, Long::sum);
        }
        return cuenta;
    }
}
