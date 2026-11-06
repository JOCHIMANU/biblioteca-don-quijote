package com.quijote.biblioteca.repositorio;

import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    Page<RegistroAuditoria> findByAccion(AccionAuditada accion, Pageable pageable);
}
