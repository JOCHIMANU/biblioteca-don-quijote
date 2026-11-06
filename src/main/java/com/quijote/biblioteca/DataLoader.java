package com.quijote.biblioteca;

import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.repositorio.LibroRepository;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.PrestamoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Carga de ejemplo: 15 libros de caballerías, 5 lectores y algunos préstamos (uno ya vencido). */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "biblioteca.datos-ejemplo", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final LibroRepository repositorioLibros;
    private final LibroService libros;
    private final LectorService lectores;
    private final PrestamoService prestamos;

    public DataLoader(LibroRepository repositorioLibros, LibroService libros,
                      LectorService lectores, PrestamoService prestamos) {
        this.repositorioLibros = repositorioLibros;
        this.libros = libros;
        this.lectores = lectores;
        this.prestamos = prestamos;
    }

    @Override
    public void run(String... args) {
        if (repositorioLibros.count() > 0) {
            return; // la biblioteca ya tiene libros: no se pisa nada
        }
        List<Libro> l = new ArrayList<>();
        l.add(libros.registrar("CAB-001", "Amadís de Gaula", "Garci Rodríguez de Montalvo", 1508));
        l.add(libros.registrar("CAB-002", "Las sergas de Esplandián", "Garci Rodríguez de Montalvo", 1510));
        l.add(libros.registrar("CAB-003", "Amadís de Grecia", "Feliciano de Silva", 1530));
        l.add(libros.registrar("CAB-004", "Florisando", "Páez de Ribera", 1510));
        l.add(libros.registrar("CAB-005", "Lisuarte de Grecia", "Juan Díaz", 1526));
        l.add(libros.registrar("CAB-006", "Tirante el Blanco", "Joanot Martorell", 1511));
        l.add(libros.registrar("CAB-007", "Palmerín de Oliva", "Anónimo", 1511));
        l.add(libros.registrar("CAB-008", "Primaleón", "Anónimo", 1512));
        l.add(libros.registrar("CAB-009", "Palmerín de Inglaterra", "Francisco de Moraes", 1547));
        l.add(libros.registrar("CAB-010", "Don Belianís de Grecia", "Jerónimo Fernández", 1547));
        l.add(libros.registrar("CAB-011", "El caballero del Febo", "Diego Ortúñez de Calahorra", 1555));
        l.add(libros.registrar("CAB-012", "Don Cirongilio de Tracia", "Bernardo de Vargas", 1545));
        l.add(libros.registrar("CAB-013", "Felixmarte de Hircania", "Melchor de Ortega", 1556));
        l.add(libros.registrar("CAB-014", "El caballero de la Cruz", "Alonso de Salazar", 1521));
        l.add(libros.registrar("CAB-015", "Clarián de Landanís", "Gabriel Velázquez de Castillo", 1518));

        Lector nicolas = lectores.registrar("Maese Nicolás", "Barbería junto a la plaza, El Toboso", "987654321", "12345678");
        Lector cura = lectores.registrar("Pero Pérez", "Casa del cura, junto a la iglesia, En un lugar de la Mancha", "912345678", "23456789");
        Lector sanson = lectores.registrar("Sansón Carrasco", "Calle de los Bachilleres, Salamanca", "955123456", "34567890");
        Lector sobrina = lectores.registrar("Antonia la sobrina", "Casa de Don Quijote, En un lugar de la Mancha", "944321987", "45678901");
        Lector rodriguez = lectores.registrar("Doña Rodríguez", "Palacio de los Duques, Aragón", "933456789", "56789012");

        LocalDate hoy = prestamos.hoy();
        // Vencido hace 8 días: prestado hace 15
        prestamos.registrarPrestamo(l.get(0).getId(), cura.getId(), hoy.minusDays(15));
        // Vencido hace 2 días: prestado hace 9
        prestamos.registrarPrestamo(l.get(5).getId(), sanson.getId(), hoy.minusDays(9));
        // En plazo: prestado hace 3 días
        prestamos.registrarPrestamo(l.get(8).getId(), nicolas.getId(), hoy.minusDays(3));
        // Devuelto a tiempo
        Prestamo aTiempo = prestamos.registrarPrestamo(l.get(2).getId(), sobrina.getId(), hoy.minusDays(30));
        prestamos.devolver(aTiempo.getId(), hoy.minusDays(25));
        // Devuelto con retraso: 3 soles de multa
        Prestamo conMulta = prestamos.registrarPrestamo(l.get(9).getId(), rodriguez.getId(), hoy.minusDays(40));
        prestamos.devolver(conMulta.getId(), hoy.minusDays(30));

        log.info("Biblioteca poblada con datos de ejemplo.");
    }
}
