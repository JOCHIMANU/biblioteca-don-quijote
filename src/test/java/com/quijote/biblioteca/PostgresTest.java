package com.quijote.biblioteca;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.servicio.AuditoriaService;
import com.quijote.biblioteca.servicio.AvisoService;
import com.quijote.biblioteca.servicio.ConfiguracionService;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.PrestamoService;
import com.quijote.biblioteca.servicio.ReporteService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La misma lógica contra un PostgreSQL de verdad (embebido, sin Docker), que es la base que se usa en la nube.
 * H2 es más permisivo que PostgreSQL: esta prueba es la que garantiza que consultas, bloqueos, fechas,
 * paginación y reportes también funcionan allí.
 */
@SpringBootTest(properties = {"decolecta.token=", "spring.jpa.hibernate.ddl-auto=update"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({RelojFijoConfig.class, CsrfEnPruebasConfig.class})
@WithMockUser(username = "docente")
@Transactional
class PostgresTest {

    private static final LocalDate HOY = RelojFijoConfig.HOY;
    private static EmbeddedPostgres postgres;

    @BeforeAll
    static void arrancarPostgres() throws IOException {
        postgres = EmbeddedPostgres.start();
    }

    @AfterAll
    static void apagarPostgres() throws IOException {
        postgres.close();
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired LibroService libros;
    @Autowired LectorService lectores;
    @Autowired PrestamoService prestamos;
    @Autowired AvisoService avisos;
    @Autowired ConfiguracionService config;
    @Autowired AuditoriaService auditoria;
    @Autowired ReporteService reportes;

    @Test
    void lasReglasDelNegocioFuncionanEnPostgres() {
        Libro amadis = libros.registrar("AMA-1", "Amadís de Gaula", "Garci Rodríguez de Montalvo", 1508);
        Libro tirante = libros.registrar("TIR-1", "Tirante el Blanco", "Joanot Martorell", 1511);
        Lector pero = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");

        // préstamo (bloqueo FOR UPDATE), doble préstamo, multa y devolución
        Prestamo p = prestamos.registrarPrestamo(amadis.getId(), pero.getId(), HOY.minusDays(12));
        assertThatThrownBy(() -> prestamos.registrarPrestamo(amadis.getId(), pero.getId(), HOY))
                .isInstanceOf(BibliotecaException.class).hasMessageContaining("tierras ajenas");
        assertThat(prestamos.morosos()).hasSize(1);
        assertThat(avisos.generarAvisos()).isEqualTo(1);
        assertThat(avisos.generarAvisos()).isZero();
        assertThat(prestamos.devolver(p.getId(), HOY)).isEqualTo(5);

        // varios libros a la vez, tope y anulación
        config.guardar(7, 1, 1, false);
        assertThatThrownBy(() -> prestamos.registrarPrestamos(List.of(amadis.getId(), tirante.getId()), pero.getId(), HOY))
                .hasMessageContaining("tope");
        config.guardar(7, 1, 0, false);
        List<Prestamo> dos = prestamos.registrarPrestamos(List.of(amadis.getId(), tirante.getId()), pero.getId(), HOY);
        assertThat(dos).hasSize(2);
        prestamos.anular(dos.get(0).getId());
        prestamos.deshacerDevolucion(p.getId()).getId();
    }

    @Test
    void laBusquedaConTildesMayusculasYComodinesFuncionaEnPostgres() {
        libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        libros.registrar("100-X", "Cien % seguro_", "Anónimo", 1500);
        lectores.registrar("Sansón Carrasco", "Salamanca", "955123456", "34567890");

        assertThat(libros.listar("AMADÍS")).hasSize(1);
        assertThat(libros.listar("amadís")).hasSize(1);
        assertThat(libros.listar("montalvo")).hasSize(1);
        assertThat(libros.listar("ama-1")).hasSize(1);
        assertThat(libros.listar("%")).hasSize(1);        // el % del usuario es una letra más, no un comodín
        assertThat(libros.listar("seguro_")).hasSize(1);
        assertThat(libros.listar("zzz")).isEmpty();
        assertThat(libros.buscarDisponibles("a", List.of())).isNotEmpty();
        assertThat(lectores.buscar("sansón")).hasSize(1);
        assertThat(lectores.buscar("3456")).hasSize(1);
    }

    @Test
    void laAuditoriaYSuPaginacionFuncionanEnPostgres() {
        for (int i = 0; i < 60; i++) {
            libros.registrar("L-" + i, "Libro " + i, "Autor", 1500);
        }
        var primera = auditoria.pagina(null, 0);
        assertThat(primera.getTotalElements()).isEqualTo(60);
        assertThat(primera.getContent()).hasSize(AuditoriaService.POR_PAGINA);
        assertThat(auditoria.pagina(null, 1).getContent()).hasSize(10);
        assertThat(auditoria.pagina(AccionAuditada.LIBRO_REGISTRADO, 0).getTotalElements()).isEqualTo(60);
        assertThat(auditoria.pagina(AccionAuditada.PRESTAMO_ANULADO, 0).getTotalElements()).isZero();
    }

    @Test
    void losReportesYLasPantallasFuncionanEnPostgres() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        Prestamo p = prestamos.registrarPrestamo(libro.getId(), lector.getId(), HOY.minusDays(12));
        prestamos.devolver(p.getId(), HOY);

        assertThat(new String(reportes.generar("multas", HOY.minusDays(30), HOY).contenido(), StandardCharsets.UTF_8))
                .contains("TOTAL (1 multa").contains(";5\r\n");
        for (String ruta : new String[]{"/", "/libros", "/libros?q=amad", "/lectores", "/prestamos", "/prestamos?todos=1",
                "/prestamos/nuevo", "/morosos", "/palomas", "/auditoria", "/configuracion", "/reportes/prestamos.csv",
                "/libros/" + libro.getId(), "/lectores/" + lector.getId()}) {
            mvc.perform(get(ruta)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/libros/disponibles").param("q", "amadís")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/lectores/buscar").param("q", "pero")).andExpect(jsonPath("$[0].etiqueta").value("Pero Pérez"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void enPostgresElRespaldoEnUnClicSeExplicaEnVezDeFallar() throws Exception {
        mvc.perform(get("/configuracion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PostgreSQL")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Descargar respaldo"))));
        mvc.perform(post("/respaldo"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("panel del proveedor")));
    }
}
