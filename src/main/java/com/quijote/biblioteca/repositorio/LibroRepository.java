package com.quijote.biblioteca.repositorio;

import com.quijote.biblioteca.modelo.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    List<Libro> findAllByOrderByTituloAsc();

    /**
     * El patrón llega ya armado (en minúsculas, con % y los comodines del usuario escapados con \):
     * así la consulta es igual en H2 y en PostgreSQL, que no admite parámetros dentro de concat().
     */
    @Query("""
            select l from Libro l
            where lower(l.titulo) like :patron escape '\\'
               or lower(l.autor) like :patron escape '\\'
               or lower(l.codigo) like :patron escape '\\'
            order by l.titulo""")
    List<Libro> buscar(@Param("patron") String patron);

    /** Cierra el libro mientras se asienta el préstamo, para que dos no lo presten a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id = :id")
    Optional<Libro> buscarParaPrestar(@Param("id") Long id);
}
