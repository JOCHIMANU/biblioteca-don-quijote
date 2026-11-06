package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.Aviso;
import com.quijote.biblioteca.modelo.EstadoAviso;
import com.quijote.biblioteca.repositorio.AvisoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** La paloma mensajera: avisos para el escudero Sancho por los préstamos vencidos. */
@Service
@Transactional(readOnly = true)
public class AvisoService {

    private static final Logger log = LoggerFactory.getLogger(AvisoService.class);

    private final AvisoRepository avisos;
    private final PrestamoService prestamos;
    private final AuditoriaService auditoria;

    public AvisoService(AvisoRepository avisos, PrestamoService prestamos, AuditoriaService auditoria) {
        this.avisos = avisos;
        this.prestamos = prestamos;
        this.auditoria = auditoria;
    }

    /** Crea un aviso por cada moroso que aún no lo tenga. Devuelve cuántos avisos nuevos hubo. */
    @Transactional
    public int generarAvisos() {
        int nuevos = 0;
        for (PrestamoVista moroso : prestamos.morosos()) {
            if (!avisos.existsByPrestamoId(moroso.prestamo().getId())) {
                avisos.save(new Aviso(moroso.prestamo(), prestamos.hoy()));
                nuevos++;
            }
        }
        return nuevos;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        log.info("Paloma mensajera al arrancar: {} aviso(s) nuevo(s).", generarAvisos());
    }

    @Scheduled(cron = "${biblioteca.cron-palomas:0 0 8 * * *}")
    public void tareaDiaria() {
        log.info("Paloma mensajera, ronda diaria: {} aviso(s) nuevo(s).", generarAvisos());
    }

    public long contarPendientes() {
        return avisos.countByEstadoAndPrestamoFechaDevolucionIsNull(EstadoAviso.PENDIENTE);
    }

    /** Avisos aún por reclamar, del mayor al menor atraso. */
    public List<AvisoVista> pendientes() {
        return avisos.findByEstadoAndPrestamoFechaDevolucionIsNullOrderByIdDesc(EstadoAviso.PENDIENTE).stream()
                .map(a -> new AvisoVista(a, prestamos.vista(a.getPrestamo()).diasAtraso()))
                .sorted(Comparator.comparingLong(AvisoVista::diasAtraso).reversed())
                .toList();
    }

    public List<Aviso> reclamadosRecientes() {
        return avisos.findTop10ByEstadoOrderByReclamadoElDescIdDesc(EstadoAviso.RECLAMADO);
    }

    @Transactional
    public void reclamar(Long avisoId) {
        Aviso aviso = avisos.findById(avisoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tal aviso no existe."));
        if (aviso.getEstado() == EstadoAviso.RECLAMADO) {
            throw new RecursoNoEncontradoException("Esa paloma ya fue despachada.");
        }
        aviso.reclamar(prestamos.hoy());
        var p = aviso.getPrestamo();
        auditoria.registrar(AccionAuditada.PALOMA_DESPACHADA, "«" + p.getLibro().getTitulo() + "» — "
                + p.getLector().getNombre() + ", " + prestamos.vista(p).diasAtraso() + " día(s) de atraso");
    }
}
