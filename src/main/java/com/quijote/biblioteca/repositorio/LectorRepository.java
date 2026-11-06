package com.quijote.biblioteca.repositorio;

import com.quijote.biblioteca.modelo.Lector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectorRepository extends JpaRepository<Lector, Long> {

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    List<Lector> findAllByOrderByNombreAsc();

    List<Lector> findTop10ByNombreContainingIgnoreCaseOrDniContainingOrderByNombreAsc(String nombre, String dni);
}
