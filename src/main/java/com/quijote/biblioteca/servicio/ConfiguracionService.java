package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.config.ReglasBiblioteca;
import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.Ajuste;
import com.quijote.biblioteca.repositorio.AjusteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de la casa editables en caliente. Si un ajuste no se ha guardado nunca,
 * vale el valor inicial de application.properties.
 */
@Service
@Transactional(readOnly = true)
public class ConfiguracionService {

    static final String PLAZO = "plazo-dias";
    static final String MULTA = "multa-por-dia";
    static final String MAX_LIBROS = "max-libros-por-lector";
    static final String BLOQUEAR_MOROSOS = "bloquear-morosos";

    private final AjusteRepository ajustes;
    private final ReglasBiblioteca iniciales;
    private final AuditoriaService auditoria;

    public ConfiguracionService(AjusteRepository ajustes, ReglasBiblioteca iniciales, AuditoriaService auditoria) {
        this.ajustes = ajustes;
        this.iniciales = iniciales;
        this.auditoria = auditoria;
    }

    public int plazoDias() {
        return entero(PLAZO, iniciales.getPlazoDias());
    }

    public int multaPorDia() {
        return entero(MULTA, iniciales.getMultaPorDia());
    }

    /** Máximo de libros que un lector puede tener a la vez; 0 = sin límite. */
    public int maxLibrosPorLector() {
        return entero(MAX_LIBROS, iniciales.getMaxLibrosPorLector());
    }

    public boolean bloquearMorosos() {
        return ajustes.findById(BLOQUEAR_MOROSOS)
                .map(a -> Boolean.parseBoolean(a.getValor()))
                .orElse(iniciales.isBloquearMorosos());
    }

    @Transactional
    public void guardar(int plazoDias, int multaPorDia, int maxLibrosPorLector, boolean bloquearMorosos) {
        if (plazoDias < 1 || plazoDias > 365) {
            throw new BibliotecaException("El plazo ha de ser de 1 a 365 días.");
        }
        if (multaPorDia < 0 || multaPorDia > 1000) {
            throw new BibliotecaException("La multa por día ha de ser de 0 a 1000 soles.");
        }
        if (maxLibrosPorLector < 0 || maxLibrosPorLector > 100) {
            throw new BibliotecaException("El tope de libros ha de ser de 0 (sin límite) a 100.");
        }
        List<String> cambios = new ArrayList<>();
        if (plazoDias != plazoDias()) {
            cambios.add("plazo " + plazoDias() + " → " + plazoDias + " días");
        }
        if (multaPorDia != multaPorDia()) {
            cambios.add("multa por día " + multaPorDia() + " → " + multaPorDia + " sol(es)");
        }
        if (maxLibrosPorLector != maxLibrosPorLector()) {
            cambios.add("tope de libros " + maxLibrosPorLector() + " → " + maxLibrosPorLector + " (0 = sin límite)");
        }
        if (bloquearMorosos != bloquearMorosos()) {
            cambios.add("bloquear morosos " + (bloquearMorosos() ? "sí" : "no") + " → " + (bloquearMorosos ? "sí" : "no"));
        }
        poner(PLAZO, Integer.toString(plazoDias));
        poner(MULTA, Integer.toString(multaPorDia));
        poner(MAX_LIBROS, Integer.toString(maxLibrosPorLector));
        poner(BLOQUEAR_MOROSOS, Boolean.toString(bloquearMorosos));
        if (!cambios.isEmpty()) {
            auditoria.registrar(AccionAuditada.CONFIGURACION, String.join("; ", cambios));
        }
    }

    private int entero(String clave, int porDefecto) {
        return ajustes.findById(clave).map(a -> {
            try {
                return Integer.parseInt(a.getValor());
            } catch (NumberFormatException e) {
                return porDefecto;
            }
        }).orElse(porDefecto);
    }

    private void poner(String clave, String valor) {
        Ajuste ajuste = ajustes.findById(clave).orElseGet(() -> new Ajuste(clave, valor));
        ajuste.setValor(valor);
        ajustes.save(ajuste);
    }
}
