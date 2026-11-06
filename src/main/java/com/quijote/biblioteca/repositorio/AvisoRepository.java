package com.quijote.biblioteca.repositorio;

import com.quijote.biblioteca.modelo.Aviso;
import com.quijote.biblioteca.modelo.EstadoAviso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    boolean existsByPrestamoId(Long prestamoId);

    void deleteByPrestamoId(Long prestamoId);

    /** Solo cuentan los avisos cuyo libro sigue fuera: si ya volvió, no hay nada que reclamar. */
    List<Aviso> findByEstadoAndPrestamoFechaDevolucionIsNullOrderByIdDesc(EstadoAviso estado);

    long countByEstadoAndPrestamoFechaDevolucionIsNull(EstadoAviso estado);

    List<Aviso> findTop10ByEstadoOrderByReclamadoElDescIdDesc(EstadoAviso estado);
}
