package com.quijote.biblioteca.repositorio;

import com.quijote.biblioteca.modelo.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    Optional<Prestamo> findByLibroIdAndFechaDevolucionIsNull(Long libroId);

    List<Prestamo> findByFechaDevolucionIsNullOrderByFechaPrestamoAsc();

    List<Prestamo> findByLectorIdAndFechaDevolucionIsNull(Long lectorId);

    long countByLectorIdAndFechaDevolucionIsNull(Long lectorId);

    boolean existsByLectorId(Long lectorId);

    boolean existsByLibroId(Long libroId);

    List<Prestamo> findByPlazoDiasIsNull();

    List<Prestamo> findByFechaDevolucionBetweenOrderByFechaDevolucionAscIdAsc(LocalDate desde, LocalDate hasta);

    List<Prestamo> findAllByOrderByFechaPrestamoDescIdDesc();

    List<Prestamo> findByLibroIdOrderByFechaPrestamoDescIdDesc(Long libroId);

    List<Prestamo> findByLectorIdOrderByFechaPrestamoDescIdDesc(Long lectorId);
}
