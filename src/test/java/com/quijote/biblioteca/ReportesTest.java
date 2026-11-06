package com.quijote.biblioteca;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.RecursoNoEncontradoException;
import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.PrestamoService;
import com.quijote.biblioteca.servicio.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "decolecta.token=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({RelojFijoConfig.class, CsrfEnPruebasConfig.class})
@WithMockUser(username = "docente")
@Transactional
class ReportesTest {

    private static final LocalDate HOY = RelojFijoConfig.HOY;

    @Autowired ReporteService reportes;
    @Autowired LibroService libros;
    @Autowired LectorService lectores;
    @Autowired PrestamoService prestamos;
    @Autowired MockMvc mvc;

    private static String texto(ReporteService.Reporte r) {
        return new String(r.contenido(), StandardCharsets.UTF_8);
    }

    @Test
    void elCsvLlevaBomPuntoYComaYEncabezadosEnEspanol() {
        libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);

        String csv = texto(reportes.generar("libros", null, null));

        assertThat(csv).startsWith("﻿Código;Título;Autor;Año de impresión;Estado;En manos de;Debe volver el\r\n");
        assertThat(csv).contains("AMA-1;Amadís de Gaula;Montalvo;1508;En el anaquel;;\r\n");
    }

    @Test
    void elNombreDelArchivoLlevaLaFecha() {
        assertThat(reportes.generar("morosos", null, null).nombreArchivo()).isEqualTo("biblioteca-morosos-" + HOY + ".csv");
    }

    @Test
    void losCamposConSeparadorOComillasSeEntrecomillan() {
        libros.registrar("AMA-1", "Amadís; el \"Doncel\"", "Montalvo", 1508);

        assertThat(texto(reportes.generar("libros", null, null))).contains("\"Amadís; el \"\"Doncel\"\"\"");
    }

    @Test
    void lasCeldasQueParecenFormulasSeNeutralizanPeroLosTelefonosNo() {
        libros.registrar("AMA-1", "=HYPERLINK(\"http://malo\")", "+cmd|calc", 1508);
        lectores.registrar("@SUMA(1)", "-1+1", "+51 987654321", "11111111");

        String libros = texto(reportes.generar("libros", null, null));
        String lectores = texto(reportes.generar("lectores", null, null));

        assertThat(libros).contains("'=HYPERLINK").contains("'+cmd|calc");
        assertThat(lectores).contains("'@SUMA(1)").contains("'-1+1").contains(";+51 987654321;");
    }

    @Test
    void elReporteDePrestamosIndicaElEstadoDeCadaUno() {
        Libro a = libros.registrar("A-1", "Uno", "X", 1500);
        Libro b = libros.registrar("B-1", "Dos", "X", 1500);
        Libro c = libros.registrar("C-1", "Tres", "X", 1500);
        Lector l = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        Prestamo devuelto = prestamos.registrarPrestamo(a.getId(), l.getId(), HOY.minusDays(12));
        prestamos.devolver(devuelto.getId(), HOY);
        prestamos.registrarPrestamo(b.getId(), l.getId(), HOY.minusDays(10));
        prestamos.registrarPrestamo(c.getId(), l.getId(), HOY.minusDays(2));

        String csv = texto(reportes.generar("prestamos", null, null));

        assertThat(csv).contains(";Devuelto\r\n").contains(";Moroso\r\n").contains(";Prestado\r\n");
        assertThat(csv).contains("A-1;Uno;Pero Pérez;12345678;" + HOY.minusDays(12) + ";7;" + HOY.minusDays(5)
                + ";" + HOY + ";5;5;Devuelto");
        assertThat(texto(reportes.generar("morosos", null, null))).contains("Dos;B-1;Pero Pérez;987654321;El Toboso")
                .doesNotContain("Tres");
    }

    @Test
    void elReporteDeMultasSumaElTotalDelPeriodo() {
        Lector l = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        Libro a = libros.registrar("A-1", "Uno", "X", 1500);
        Libro b = libros.registrar("B-1", "Dos", "X", 1500);
        Libro c = libros.registrar("C-1", "Tres", "X", 1500);
        prestamos.devolver(prestamos.registrarPrestamo(a.getId(), l.getId(), HOY.minusDays(12)).getId(), HOY);          // 5
        prestamos.devolver(prestamos.registrarPrestamo(b.getId(), l.getId(), HOY.minusDays(10)).getId(), HOY.minusDays(1)); // 2
        prestamos.devolver(prestamos.registrarPrestamo(c.getId(), l.getId(), HOY.minusDays(3)).getId(), HOY);           // 0: no entra

        String csv = texto(reportes.generar("multas", HOY.minusDays(30), HOY));

        assertThat(csv).contains(";Uno;").contains(";Dos;").doesNotContain(";Tres;");
        assertThat(csv).contains("TOTAL (2 multas, del " + HOY.minusDays(30) + " al " + HOY + ");;;;;;7\r\n");
        // un periodo sin multas da total 0 y no falla
        assertThat(texto(reportes.generar("multas", HOY.minusDays(100), HOY.minusDays(90)))).contains("TOTAL (0 multas").contains(";0\r\n");
    }

    @Test
    void sinFechasLasMultasVanDesdeElDiaUnoDelMesHastaHoy() {
        assertThat(reportes.generar("multas", null, null).nombreArchivo())
                .isEqualTo("biblioteca-multas-" + HOY.withDayOfMonth(1) + "_a_" + HOY + ".csv");
    }

    @Test
    void fechasAlReveszOReporteInexistenteSeRechazan() {
        assertThatThrownBy(() -> reportes.generar("multas", HOY, HOY.minusDays(1)))
                .isInstanceOf(BibliotecaException.class).hasMessageContaining("desde");
        assertThatThrownBy(() -> reportes.generar("secretos", null, null)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void laDescargaPorHttpTraeTipoNombreYSinCache() throws Exception {
        libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);

        mvc.perform(get("/reportes/libros.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("biblioteca-libros-")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Amadís de Gaula")));
        mvc.perform(get("/reportes/secretos.csv")).andExpect(status().isNotFound());
        mvc.perform(get("/reportes/multas.csv").param("desde", HOY.toString()).param("hasta", HOY.minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no puede ser posterior")));
    }
}
