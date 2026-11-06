package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.RegistroAuditoria;
import com.quijote.biblioteca.repositorio.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Libro de auditoría básico: cada acción importante deja una línea con fecha, detalle y origen.
 * Se registra dentro de la misma transacción que la acción: si ésta se deshace, el asiento también.
 */
@Service
@Transactional
public class AuditoriaService {

    public static final int POR_PAGINA = 50;
    private static final int MAX_DETALLE = 600;

    private final AuditoriaRepository registros;
    private final Clock reloj;

    public AuditoriaService(AuditoriaRepository registros, Clock reloj) {
        this.registros = registros;
        this.reloj = reloj;
    }

    public void registrar(AccionAuditada accion, String detalle) {
        String texto = detalle == null ? "" : detalle;
        if (texto.length() > MAX_DETALLE) {
            texto = texto.substring(0, MAX_DETALLE - 1) + "…";
        }
        registros.save(new RegistroAuditoria(LocalDateTime.now(reloj), accion, texto, origenActual()));
    }

    /** Una página del libro, de lo más reciente a lo más antiguo; {@code accion} nula = todas. */
    @Transactional(readOnly = true)
    public Page<RegistroAuditoria> pagina(AccionAuditada accion, int pagina) {
        PageRequest pedido = PageRequest.of(Math.max(0, pagina), POR_PAGINA,
                Sort.by(Sort.Direction.DESC, "momento", "id"));
        return accion == null ? registros.findAll(pedido) : registros.findByAccion(accion, pedido);
    }

    /**
     * Quién y desde dónde hace la petición en curso («admin · este equipo»); nada si la acción la inicia el
     * propio sistema (tareas programadas). El texto cabe en la columna de 60 caracteres que ya existe.
     */
    private static String origenActual() {
        String usuario = null;
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.isAuthenticated() && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            usuario = autenticacion.getName();
        }
        String direccion = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest peticion = atributos.getRequest();
            String ip = peticion.getRemoteAddr();
            if (ip != null) {
                direccion = ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "127.0.0.1".equals(ip)) ? "este equipo" : ip;
            }
        }
        if (usuario == null && direccion == null) {
            return null;
        }
        String origen = usuario == null ? direccion : (direccion == null ? usuario : usuario + " · " + direccion);
        return origen.length() > 60 ? origen.substring(0, 59) + "…" : origen;
    }
}
