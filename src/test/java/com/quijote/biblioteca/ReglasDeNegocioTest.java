package com.quijote.biblioteca;

import com.quijote.biblioteca.controlador.LectorForm;
import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.excepcion.CodigoDuplicadoException;
import com.quijote.biblioteca.excepcion.DniDuplicadoException;
import com.quijote.biblioteca.excepcion.LibroYaPrestadoException;
import com.quijote.biblioteca.excepcion.ReglaDePrestamoException;
import com.quijote.biblioteca.modelo.EstadoAviso;
import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.modelo.Prestamo;
import com.quijote.biblioteca.servicio.AvisoService;
import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.servicio.AuditoriaService;
import com.quijote.biblioteca.servicio.AvisoVista;
import com.quijote.biblioteca.servicio.ConfiguracionService;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.LibroVista;
import com.quijote.biblioteca.servicio.PrestamoService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(RelojFijoConfig.class)
@Transactional
class ReglasDeNegocioTest {

    private static final LocalDate HOY = RelojFijoConfig.HOY;

    @Autowired LibroService libros;
    @Autowired LectorService lectores;
    @Autowired PrestamoService prestamos;
    @Autowired AvisoService avisos;
    @Autowired ConfiguracionService config;
    @Autowired AuditoriaService auditoria;
    @Autowired Validator validador;

    private Libro libro(String codigo) {
        return libros.registrar(codigo, "Libro " + codigo, "Autor", 1508);
    }

    private Lector lector(String dni) {
        return lectores.registrar("Lector " + dni, "La Mancha", "987654321", dni);
    }

    // ---------------------------------------------------------- doble préstamo

    @Test
    void unLibroPrestadoNoSePuedePrestarDeNuevo() {
        Libro amadis = libro("AMA-1");
        Lector uno = lector("11111111");
        Lector otro = lector("22222222");
        prestamos.registrarPrestamo(amadis.getId(), uno.getId(), HOY);

        assertThatThrownBy(() -> prestamos.registrarPrestamo(amadis.getId(), otro.getId(), HOY))
                .isInstanceOf(LibroYaPrestadoException.class)
                .hasMessageContaining("tierras ajenas")
                .hasMessageContaining(uno.getNombre());
    }

    @Test
    void tras_devolverlo_el_libro_puede_prestarse_otra_vez() {
        Libro amadis = libro("AMA-1");
        Lector uno = lector("11111111");
        Lector otro = lector("22222222");
        Prestamo p = prestamos.registrarPrestamo(amadis.getId(), uno.getId(), HOY.minusDays(2));
        prestamos.devolver(p.getId(), HOY);

        Prestamo nuevo = prestamos.registrarPrestamo(amadis.getId(), otro.getId(), HOY);

        assertThat(nuevo.getLector().getId()).isEqualTo(otro.getId());
    }

    // ------------------------------------------------- varios libros a la vez

    @Test
    void variosLibrosSePrestanAUnLectorComoPrestamosIndependientes() {
        Libro a = libro("A-1");
        Libro b = libro("B-1");
        Libro c = libro("C-1");
        Lector lector = lector("11111111");

        List<Prestamo> asentados = prestamos.registrarPrestamos(
                List.of(a.getId(), b.getId(), c.getId()), lector.getId(), HOY.minusDays(10));

        assertThat(asentados).hasSize(3);
        assertThat(prestamos.abiertos()).hasSize(3);
        assertThat(estadoDe(a)).isEqualTo("Prestado");
        assertThat(estadoDe(c)).isEqualTo("Prestado");
        // cada uno tiene su propio plazo y su propia multa: se devuelve uno solo y los otros siguen fuera
        assertThat(prestamos.devolver(asentados.get(0).getId(), HOY)).isEqualTo(3);
        assertThat(prestamos.abiertos()).hasSize(2);
        assertThat(estadoDe(a)).isEqualTo("En el anaquel");
        assertThat(estadoDe(b)).isEqualTo("Prestado");
    }

    @Test
    void siUnLibroDeLaListaYaEstaPrestadoNoSeAsientaNinguno() {
        Libro libre = libro("A-1");
        Libro ocupado = libro("B-1");
        Lector uno = lector("11111111");
        Lector otro = lector("22222222");
        prestamos.registrarPrestamo(ocupado.getId(), uno.getId(), HOY);

        assertThatThrownBy(() -> prestamos.registrarPrestamos(
                List.of(libre.getId(), ocupado.getId()), otro.getId(), HOY))
                .isInstanceOf(LibroYaPrestadoException.class);

        assertThat(estadoDe(libre)).isEqualTo("En el anaquel");
        assertThat(prestamos.abiertos()).hasSize(1);
    }

    @Test
    void unLibroRepetidoEnLaListaSePrestaUnaSolaVez() {
        Libro a = libro("A-1");
        Lector lector = lector("11111111");

        assertThat(prestamos.registrarPrestamos(List.of(a.getId(), a.getId()), lector.getId(), HOY)).hasSize(1);
    }

    @Test
    void sinLibrosNoHayPrestamo() {
        Lector lector = lector("11111111");
        assertThatThrownBy(() -> prestamos.registrarPrestamos(List.of(), lector.getId(), HOY))
                .isInstanceOf(BibliotecaException.class).hasMessageContaining("al menos un libro");
    }

    @Test
    void elEstadoDelLibroCambiaAlPrestarseYAlDevolverse() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        assertThat(estadoDe(amadis)).isEqualTo("En el anaquel");

        Prestamo p = prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY);
        assertThat(estadoDe(amadis)).isEqualTo("Prestado");

        prestamos.devolver(p.getId(), HOY);
        assertThat(estadoDe(amadis)).isEqualTo("En el anaquel");
    }

    private String estadoDe(Libro libro) {
        return libros.listar(null).stream()
                .filter(v -> v.libro().getId().equals(libro.getId()))
                .map(LibroVista::estado).findFirst().orElseThrow();
    }

    @Test
    void noSePuedeAsentarUnPrestamoConFechaFutura() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        assertThatThrownBy(() -> prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.plusDays(1)))
                .isInstanceOf(BibliotecaException.class);
    }

    // ------------------------------------------------------------------ multa

    @Test
    void multaCeroSiSeDevuelveDentroDelPlazoOJustoAlSeptimoDia() {
        LocalDate prestado = LocalDate.of(2026, 10, 1);
        assertThat(prestamos.calcularMulta(prestado, prestado)).isZero();
        assertThat(prestamos.calcularMulta(prestado, prestado.plusDays(3))).isZero();
        assertThat(prestamos.calcularMulta(prestado, prestado.plusDays(7))).isZero();
    }

    @Test
    void multaDeUnSolSiSeRetrasaUnDia() {
        LocalDate prestado = LocalDate.of(2026, 10, 1);
        assertThat(prestamos.calcularMulta(prestado, prestado.plusDays(8))).isEqualTo(1);
    }

    @Test
    void multaDeUnSolPorCadaDiaDeRetraso() {
        LocalDate prestado = LocalDate.of(2026, 10, 1);
        assertThat(prestamos.calcularMulta(prestado, prestado.plusDays(10))).isEqualTo(3);
        assertThat(prestamos.calcularMulta(prestado, prestado.plusDays(30))).isEqualTo(23);
    }

    @Test
    void devolverGuardaLaMultaCalculada() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        Prestamo p = prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.minusDays(12));

        int multa = prestamos.devolver(p.getId(), HOY);

        assertThat(multa).isEqualTo(5);
        assertThat(prestamos.obtener(p.getId()).getMulta()).isEqualTo(5);
        assertThat(prestamos.obtener(p.getId()).getFechaDevolucion()).isEqualTo(HOY);
    }

    @Test
    void noSeDevuelveDosVecesNiAntesDeHaberPrestado() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        Prestamo p = prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.minusDays(3));

        assertThatThrownBy(() -> prestamos.devolver(p.getId(), HOY.minusDays(4))).isInstanceOf(BibliotecaException.class);
        prestamos.devolver(p.getId(), HOY);
        assertThatThrownBy(() -> prestamos.devolver(p.getId(), HOY)).isInstanceOf(BibliotecaException.class);
    }

    // ------------------------------------------------------ códigos y DNI únicos

    @Test
    void elCodigoDelLomoNoSePuedeRepetir() {
        libro("AMA-1");
        assertThatThrownBy(() -> libros.registrar("AMA-1", "Otro", "Otro", 1500))
                .isInstanceOf(CodigoDuplicadoException.class);
        // sin distinguir mayúsculas ni espacios
        assertThatThrownBy(() -> libros.registrar("  ama-1 ", "Otro", "Otro", 1500))
                .isInstanceOf(CodigoDuplicadoException.class);
    }

    @Test
    void elDniNoSePuedeRepetir() {
        lector("11111111");
        assertThatThrownBy(() -> lectores.registrar("Otro", "Otro lugar", "999999999", "11111111"))
                .isInstanceOf(DniDuplicadoException.class);
    }

    @Test
    void elDniDebeTenerOchoDigitos() {
        assertThat(erroresDeDni("1234567")).isNotEmpty();
        assertThat(erroresDeDni("123456789")).isNotEmpty();
        assertThat(erroresDeDni("1234567a")).isNotEmpty();
        assertThat(erroresDeDni("12345678")).isEmpty();
    }

    private List<String> erroresDeDni(String dni) {
        LectorForm f = new LectorForm();
        f.setNombre("Sancho");
        f.setDomicilio("La Mancha");
        f.setCelular("987654321");
        f.setDni(dni);
        return validador.validate(f).stream()
                .filter(v -> v.getPropertyPath().toString().equals("dni"))
                .map(v -> v.getMessage()).toList();
    }

    // ------------------------------------------------------ configuración

    @Test
    void elTopeDeLibrosImpideLlevarseMasDeLoPermitido() {
        config.guardar(7, 1, 2, false);
        Lector lector = lector("11111111");
        Libro a = libro("A-1"), b = libro("B-1"), c = libro("C-1");
        prestamos.registrarPrestamo(a.getId(), lector.getId(), HOY);

        assertThatThrownBy(() -> prestamos.registrarPrestamos(List.of(b.getId(), c.getId()), lector.getId(), HOY))
                .isInstanceOf(ReglaDePrestamoException.class)
                .hasMessageContaining("tope").hasMessageContaining("solo caben 1");
        assertThat(prestamos.abiertos()).hasSize(1); // no se asentó ninguno
        prestamos.registrarPrestamo(b.getId(), lector.getId(), HOY);       // el segundo sí cabe
        assertThatThrownBy(() -> prestamos.registrarPrestamo(c.getId(), lector.getId(), HOY))
                .isInstanceOf(ReglaDePrestamoException.class).hasMessageContaining("devuelva");
    }

    @Test
    void conTopeCeroNoHayLimite() {
        config.guardar(7, 1, 0, false);
        Lector lector = lector("11111111");
        List<Long> ids = List.of(libro("A-1").getId(), libro("B-1").getId(), libro("C-1").getId(), libro("D-1").getId());

        assertThat(prestamos.registrarPrestamos(ids, lector.getId(), HOY)).hasSize(4);
    }

    @Test
    void siSeActivaElBloqueoUnMorosoNoPuedeLlevarseMasLibros() {
        config.guardar(7, 1, 0, true);
        Lector moroso = lector("11111111");
        Lector puntual = lector("22222222");
        Libro viejo = libro("A-1"), nuevo = libro("B-1"), otro = libro("C-1");
        prestamos.registrarPrestamo(viejo.getId(), moroso.getId(), HOY.minusDays(10)); // vencido
        prestamos.registrarPrestamo(otro.getId(), puntual.getId(), HOY.minusDays(2));  // en plazo

        assertThatThrownBy(() -> prestamos.registrarPrestamo(nuevo.getId(), moroso.getId(), HOY))
                .isInstanceOf(ReglaDePrestamoException.class).hasMessageContaining("vencido");
        assertThat(prestamos.registrarPrestamo(nuevo.getId(), puntual.getId(), HOY)).isNotNull();
    }

    @Test
    void sinElBloqueoUnMorosoPuedeSeguirLlevandoLibros() {
        config.guardar(7, 1, 0, false);
        Lector moroso = lector("11111111");
        prestamos.registrarPrestamo(libro("A-1").getId(), moroso.getId(), HOY.minusDays(10));

        assertThat(prestamos.registrarPrestamo(libro("B-1").getId(), moroso.getId(), HOY)).isNotNull();
    }

    @Test
    void cambiarLaConfiguracionNoAlteraLosPrestamosEnCurso() {
        Lector lector = lector("11111111");
        Prestamo viejo = prestamos.registrarPrestamo(libro("A-1").getId(), lector.getId(), HOY.minusDays(12)); // 7 días, 1 sol

        config.guardar(14, 5, 0, false); // ahora: 14 días y 5 soles por día
        Prestamo nuevo = prestamos.registrarPrestamo(libro("B-1").getId(), lector.getId(), HOY.minusDays(12));

        assertThat(prestamos.fechaLimite(viejo)).isEqualTo(HOY.minusDays(12).plusDays(7));
        assertThat(prestamos.fechaLimite(nuevo)).isEqualTo(HOY.minusDays(12).plusDays(14));
        assertThat(prestamos.devolver(viejo.getId(), HOY)).isEqualTo(5);  // 12 − 7 = 5 días × 1 sol
        assertThat(prestamos.devolver(nuevo.getId(), HOY)).isZero();      // aún dentro de sus 14 días
        assertThat(prestamos.morosos()).isEmpty();
    }

    @Test
    void laMultaNuevaSeAplicaAlosPrestamosNuevos() {
        config.guardar(7, 3, 0, false);
        Lector lector = lector("11111111");
        Prestamo p = prestamos.registrarPrestamo(libro("A-1").getId(), lector.getId(), HOY.minusDays(10));

        assertThat(prestamos.devolver(p.getId(), HOY)).isEqualTo(9); // 3 días × 3 soles
    }

    @Test
    void laConfiguracionRechazaValoresAbsurdos() {
        assertThatThrownBy(() -> config.guardar(0, 1, 0, false)).isInstanceOf(BibliotecaException.class);
        assertThatThrownBy(() -> config.guardar(7, -1, 0, false)).isInstanceOf(BibliotecaException.class);
        assertThatThrownBy(() -> config.guardar(7, 1, -3, false)).isInstanceOf(BibliotecaException.class);
        assertThat(config.plazoDias()).isEqualTo(7); // sin cambios
    }

    // ---------------------------------------------- editar, borrar y anular

    @Test
    void sePuedeCorregirUnLibroPeroSuCodigoSigueSiendoUnico() {
        Libro a = libro("A-1");
        libro("B-1");

        libros.actualizar(a.getId(), " a-9 ", "Título corregido", "Autor corregido", 1510);
        assertThat(libros.obtener(a.getId()).getCodigo()).isEqualTo("A-9");
        assertThat(libros.obtener(a.getId()).getTitulo()).isEqualTo("Título corregido");
        // dejar su propio código no es un duplicado
        libros.actualizar(a.getId(), "A-9", "Otra vez", "Autor", 1510);
        assertThatThrownBy(() -> libros.actualizar(a.getId(), "b-1", "X", "Y", 1500))
                .isInstanceOf(CodigoDuplicadoException.class);
    }

    @Test
    void sePuedeCorregirUnLectorPeroSuDniSigueSiendoUnico() {
        Lector a = lector("11111111");
        lector("22222222");

        lectores.actualizar(a.getId(), "Nombre corregido", "Otro lugar", "999888777", "11111111");
        assertThat(lectores.obtener(a.getId()).getNombre()).isEqualTo("Nombre corregido");
        assertThatThrownBy(() -> lectores.actualizar(a.getId(), "N", "D", "999888777", "22222222"))
                .isInstanceOf(DniDuplicadoException.class);
    }

    @Test
    void soloSeBorraLoQueNoTieneHistorial() {
        Libro sinHistoria = libro("A-1");
        Libro conHistoria = libro("B-1");
        Lector lectorSinHistoria = lector("11111111");
        Lector lectorConHistoria = lector("22222222");
        prestamos.registrarPrestamo(conHistoria.getId(), lectorConHistoria.getId(), HOY);

        libros.eliminar(sinHistoria.getId());
        lectores.eliminar(lectorSinHistoria.getId());
        assertThat(libros.listar(null)).hasSize(1);
        assertThatThrownBy(() -> libros.eliminar(conHistoria.getId())).isInstanceOf(BibliotecaException.class);
        assertThatThrownBy(() -> lectores.eliminar(lectorConHistoria.getId())).isInstanceOf(BibliotecaException.class);
    }

    @Test
    void anularUnPrestamoLiberaElLibroYBorraSuAviso() {
        Libro libro = libro("A-1");
        Lector lector = lector("11111111");
        Prestamo p = prestamos.registrarPrestamo(libro.getId(), lector.getId(), HOY.minusDays(10));
        avisos.generarAvisos();
        assertThat(avisos.contarPendientes()).isEqualTo(1);

        prestamos.anular(p.getId());

        assertThat(estadoDe(libro)).isEqualTo("En el anaquel");
        assertThat(prestamos.abiertos()).isEmpty();
        assertThat(avisos.contarPendientes()).isZero();
        assertThat(avisos.generarAvisos()).isZero();
        // y ahora sí se puede borrar el libro: el asiento anulado no deja historial
        libros.eliminar(libro.getId());
    }

    @Test
    void unPrestamoYaDevueltoNoSeAnulaPeroSiSeDeshaceSuDevolucion() {
        Libro libro = libro("A-1");
        Lector lector = lector("11111111");
        Prestamo p = prestamos.registrarPrestamo(libro.getId(), lector.getId(), HOY.minusDays(12));
        prestamos.devolver(p.getId(), HOY); // multa de 5 soles

        assertThatThrownBy(() -> prestamos.anular(p.getId())).isInstanceOf(BibliotecaException.class);

        prestamos.deshacerDevolucion(p.getId());
        assertThat(estadoDe(libro)).isEqualTo("Prestado");
        assertThat(prestamos.obtener(p.getId()).getMulta()).isNull();
        assertThat(prestamos.obtener(p.getId()).getFechaDevolucion()).isNull();
        assertThat(prestamos.morosos()).hasSize(1); // vuelve a estar vencido
        assertThatThrownBy(() -> prestamos.deshacerDevolucion(p.getId())).isInstanceOf(BibliotecaException.class);
    }

    @Test
    void noSeDeshaceUnaDevolucionSiElLibroYaSePrestoDeNuevo() {
        Libro libro = libro("A-1");
        Lector uno = lector("11111111");
        Lector otro = lector("22222222");
        Prestamo primero = prestamos.registrarPrestamo(libro.getId(), uno.getId(), HOY.minusDays(3));
        prestamos.devolver(primero.getId(), HOY.minusDays(2));
        prestamos.registrarPrestamo(libro.getId(), otro.getId(), HOY);

        assertThatThrownBy(() -> prestamos.deshacerDevolucion(primero.getId()))
                .isInstanceOf(BibliotecaException.class).hasMessageContaining(otro.getNombre());
    }

    // -------------------------------------------------------------- auditoría

    private List<String> asientos(AccionAuditada accion) {
        return auditoria.pagina(accion, 0).getContent().stream().map(r -> r.getDetalle()).toList();
    }

    @Test
    void cadaAccionImportanteDejaSuAsientoEnLaAuditoria() {
        Libro libro = libro("A-1");
        Lector lector = lector("11111111");
        assertThat(asientos(AccionAuditada.LIBRO_REGISTRADO)).singleElement().asString().contains("A-1");
        assertThat(asientos(AccionAuditada.LECTOR_INSCRITO)).singleElement().asString().contains("11111111");

        Prestamo p = prestamos.registrarPrestamo(libro.getId(), lector.getId(), HOY.minusDays(10));
        assertThat(asientos(AccionAuditada.PRESTAMO_ASENTADO)).singleElement().asString()
                .contains("Libro A-1").contains("Lector 11111111").contains("plazo 7 días");

        prestamos.devolver(p.getId(), HOY);
        assertThat(asientos(AccionAuditada.DEVOLUCION)).singleElement().asString().contains("multa: 3 sol(es)");

        prestamos.deshacerDevolucion(p.getId());
        assertThat(asientos(AccionAuditada.DEVOLUCION_DESHECHA)).singleElement().asString().contains("multa 3");

        prestamos.anular(p.getId());
        assertThat(asientos(AccionAuditada.PRESTAMO_ANULADO)).hasSize(1);

        libros.actualizar(libro.getId(), "A-2", "Corregido", "Autor", 1500);
        assertThat(asientos(AccionAuditada.LIBRO_EDITADO)).singleElement().asString().contains("A-1").contains("A-2").contains("→");
        lectores.actualizar(lector.getId(), "Nombre nuevo", "Lugar", "999888777", "11111111");
        assertThat(asientos(AccionAuditada.LECTOR_EDITADO)).hasSize(1);

        libros.eliminar(libro.getId());
        lectores.eliminar(lector.getId());
        assertThat(asientos(AccionAuditada.LIBRO_BORRADO)).hasSize(1);
        assertThat(asientos(AccionAuditada.LECTOR_BORRADO)).hasSize(1);
    }

    @Test
    void laConfiguracionSoloSeAsientaSiCambioAlgoYDicePorCuanto() {
        config.guardar(7, 1, 0, false); // igual que lo vigente: nada que asentar
        assertThat(asientos(AccionAuditada.CONFIGURACION)).isEmpty();

        config.guardar(10, 1, 3, true);
        assertThat(asientos(AccionAuditada.CONFIGURACION)).singleElement().asString()
                .contains("plazo 7 → 10").contains("tope de libros 0 → 3").contains("bloquear morosos no → sí")
                .doesNotContain("multa");
    }

    @Test
    void laPalomaDespachadaQuedaAsentada() {
        Libro libro = libro("A-1");
        Lector lector = lector("11111111");
        prestamos.registrarPrestamo(libro.getId(), lector.getId(), HOY.minusDays(9));
        avisos.generarAvisos();

        avisos.reclamar(avisos.pendientes().get(0).aviso().getId());

        assertThat(asientos(AccionAuditada.PALOMA_DESPACHADA)).singleElement().asString().contains("2 día(s) de atraso");
    }

    @Test
    void laAuditoriaSePuedeFiltrarYVieneDeLoMasRecienteALoMasAntiguo() {
        libro("A-1");
        libro("B-1");
        lector("11111111");

        var todas = auditoria.pagina(null, 0).getContent();
        assertThat(todas).hasSize(3);
        assertThat(todas.get(0).getId()).isGreaterThan(todas.get(2).getId());
        assertThat(auditoria.pagina(AccionAuditada.LIBRO_REGISTRADO, 0).getTotalElements()).isEqualTo(2);
        assertThat(auditoria.pagina(AccionAuditada.PRESTAMO_ANULADO, 0).getTotalElements()).isZero();
    }

    // ----------------------------------------------------------------- avisos

    @Test
    void unPrestamoVencidoGeneraUnAvisoParaSancho() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.minusDays(10));

        assertThat(avisos.generarAvisos()).isEqualTo(1);

        List<AvisoVista> pendientes = avisos.pendientes();
        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).diasAtraso()).isEqualTo(3);
        assertThat(pendientes.get(0).aviso().getPrestamo().getLibro().getTitulo()).isEqualTo("Libro AMA-1");
        assertThat(pendientes.get(0).aviso().getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(avisos.contarPendientes()).isEqualTo(1);
    }

    @Test
    void noSeDuplicanLosAvisosDelMismoPrestamo() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.minusDays(10));

        assertThat(avisos.generarAvisos()).isEqualTo(1);
        assertThat(avisos.generarAvisos()).isZero();
        assertThat(avisos.pendientes()).hasSize(1);
    }

    @Test
    void unPrestamoEnPlazoNoGeneraAviso() {
        Libro amadis = libro("AMA-1");
        Libro tirante = libro("TIR-1");
        Lector lector = lector("11111111");
        prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.minusDays(7));  // vence hoy: aún no es moroso
        prestamos.registrarPrestamo(tirante.getId(), lector.getId(), HOY.minusDays(2));

        assertThat(avisos.generarAvisos()).isZero();
        assertThat(avisos.contarPendientes()).isZero();
    }

    @Test
    void sanchoPuedeMarcarUnAvisoComoReclamado() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.minusDays(9));
        avisos.generarAvisos();
        Long id = avisos.pendientes().get(0).aviso().getId();

        avisos.reclamar(id);

        assertThat(avisos.contarPendientes()).isZero();
        assertThat(avisos.reclamadosRecientes()).hasSize(1);
        assertThat(avisos.reclamadosRecientes().get(0).getReclamadoEl()).isEqualTo(HOY);
        // reclamado queda reclamado: no vuelve a aparecer como aviso nuevo
        assertThat(avisos.generarAvisos()).isZero();
    }

    @Test
    void siElLibroVuelveElAvisoYaNoPendeSobreSancho() {
        Libro amadis = libro("AMA-1");
        Lector lector = lector("11111111");
        Prestamo p = prestamos.registrarPrestamo(amadis.getId(), lector.getId(), HOY.minusDays(9));
        avisos.generarAvisos();
        assertThat(avisos.contarPendientes()).isEqualTo(1);

        prestamos.devolver(p.getId(), HOY);

        assertThat(avisos.contarPendientes()).isZero();
        assertThat(avisos.pendientes()).isEmpty();
    }

    // --------------------------------------------------------------- consultas

    @Test
    void losMorososVanOrdenadosPorDiasDeAtraso() {
        Libro a = libro("A-1");
        Libro b = libro("B-1");
        Libro c = libro("C-1");
        Lector lector = lector("11111111");
        prestamos.registrarPrestamo(a.getId(), lector.getId(), HOY.minusDays(9));   // 2 de atraso
        prestamos.registrarPrestamo(b.getId(), lector.getId(), HOY.minusDays(20));  // 13 de atraso
        prestamos.registrarPrestamo(c.getId(), lector.getId(), HOY.minusDays(3));   // en plazo

        assertThat(prestamos.morosos()).extracting(v -> v.prestamo().getLibro().getCodigo())
                .containsExactly("B-1", "A-1");
        assertThat(prestamos.abiertos()).hasSize(3);
    }
}
