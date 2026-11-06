package com.quijote.biblioteca;

import com.quijote.biblioteca.modelo.Lector;
import com.quijote.biblioteca.modelo.Libro;
import com.quijote.biblioteca.servicio.LectorService;
import com.quijote.biblioteca.servicio.LibroService;
import com.quijote.biblioteca.servicio.PrestamoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorre las pantallas con datos reales para cazar errores de plantilla. */
// El token vacío va aquí y no en application-test.properties: config/application.properties (fuera del
// proyecto empaquetado) pesa más, y las pruebas jamás deben gastar consultas del token real.
@SpringBootTest(properties = "decolecta.token=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({RelojFijoConfig.class, CsrfEnPruebasConfig.class})
@WithMockUser(username = "docente")
@Transactional
class PaginasTest {

    @Autowired MockMvc mvc;
    @Autowired LibroService libros;
    @Autowired LectorService lectores;
    @Autowired PrestamoService prestamos;

    @Test
    void todasLasPantallasResponden() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        var prestamo = prestamos.registrarPrestamo(libro.getId(), lector.getId(), RelojFijoConfig.HOY.minusDays(10));

        for (String ruta : new String[]{"/", "/libros", "/libros?q=ama", "/lectores", "/prestamos", "/prestamos?todos=1",
                "/prestamos/nuevo", "/morosos", "/palomas", "/libros/" + libro.getId(), "/lectores/" + lector.getId(),
                "/prestamos/" + prestamo.getId() + "/devolver"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk());
        }
        mvc.perform(get("/libros/999999")).andExpect(status().isNotFound());
    }

    @Test
    void elPanelDePalomasMuestraElAvisoDelMoroso() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        prestamos.registrarPrestamo(libro.getId(), lector.getId(), RelojFijoConfig.HOY.minusDays(10));

        mvc.perform(get("/palomas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Despachar paloma")))
                .andExpect(content().string(containsString("Pero Pérez")))
                .andExpect(content().string(containsString("987654321")));
    }

    @Test
    void elFormularioRechazaUnDoblePrestamoConMensajeClaro() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector uno = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        Lector otro = lectores.registrar("Maese Nicolás", "El Toboso", "987654322", "87654321");
        prestamos.registrarPrestamo(libro.getId(), uno.getId(), RelojFijoConfig.HOY);

        mvc.perform(post("/prestamos")
                        .param("libroIds", libro.getId().toString())
                        .param("lectorId", otro.getId().toString())
                        .param("fecha", RelojFijoConfig.HOY.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tierras ajenas")));
    }

    @Test
    void variosLibrosEnUnSoloPrestamoSeAsientanTodosJuntos() throws Exception {
        Libro a = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Libro b = libros.registrar("TIR-1", "Tirante el Blanco", "Martorell", 1511);
        Libro c = libros.registrar("PAL-1", "Palmerín de Oliva", "Anónimo", 1511);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");

        mvc.perform(post("/prestamos")
                        .param("libroIds", a.getId().toString(), b.getId().toString(), c.getId().toString())
                        .param("lectorId", lector.getId().toString())
                        .param("fecha", RelojFijoConfig.HOY.toString()))
                .andExpect(redirectedUrl("/prestamos"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("ok", containsString("3 préstamos asentados")));

        org.assertj.core.api.Assertions.assertThat(prestamos.abiertos()).hasSize(3);
    }

    @Test
    void siUnLibroDeLaListaYaEstaPrestadoNoSeAsientaNingunoYSeConservanLasEtiquetas() throws Exception {
        Libro libre = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Libro ocupado = libros.registrar("TIR-1", "Tirante el Blanco", "Martorell", 1511);
        Lector uno = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        Lector otro = lectores.registrar("Maese Nicolás", "El Toboso", "987654322", "87654321");
        prestamos.registrarPrestamo(ocupado.getId(), uno.getId(), RelojFijoConfig.HOY);

        mvc.perform(post("/prestamos")
                        .param("libroIds", libre.getId().toString(), ocupado.getId().toString())
                        .param("lectorId", otro.getId().toString())
                        .param("fecha", RelojFijoConfig.HOY.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tierras ajenas")))
                .andExpect(content().string(containsString("AMA-1 · Amadís de Gaula")));

        org.assertj.core.api.Assertions.assertThat(prestamos.abiertos()).hasSize(1); // solo el previo
    }

    @Test
    void elFormularioExigeAlMenosUnLibro() throws Exception {
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");

        mvc.perform(post("/prestamos")
                        .param("lectorId", lector.getId().toString())
                        .param("fecha", RelojFijoConfig.HOY.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("al menos un libro")));
    }

    @Test
    void laBusquedaPuedeExcluirLosLibrosYaAgregados() throws Exception {
        Libro a = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        libros.registrar("AMA-2", "Amadís de Grecia", "Silva", 1530);

        mvc.perform(get("/api/libros/disponibles").param("q", "amadís").param("excluir", a.getId().toString()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].etiqueta").value("AMA-2 · Amadís de Grecia"));
    }

    @Test
    void elFormularioDeLectoresRechazaUnDniCorto() throws Exception {
        mvc.perform(post("/lectores")
                        .param("nombre", "Sancho").param("domicilio", "La Mancha")
                        .param("celular", "987654321").param("dni", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ocho guarismos")));
    }

    @Test
    void laBusquedaDeLibrosSoloOfreceLosQueEstanEnElAnaquelYLimitaA10() throws Exception {
        Libro prestado = libros.registrar("AMA-0", "Amadís prestado", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        prestamos.registrarPrestamo(prestado.getId(), lector.getId(), RelojFijoConfig.HOY);
        for (int i = 1; i <= 15; i++) {
            libros.registrar("AMA-" + i, "Amadís número " + i, "Montalvo", 1508);
        }

        mvc.perform(get("/api/libros/disponibles").param("q", "amadís"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("prestado"))));
        mvc.perform(get("/api/libros/disponibles").param("q", "AMA-7"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].etiqueta").value("AMA-7 · Amadís número 7"));
        mvc.perform(get("/api/libros/disponibles").param("q", "zzzz"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void laBusquedaDeLectoresFiltraPorNombreOPorDni() throws Exception {
        lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        lectores.registrar("Maese Nicolás", "El Toboso", "987654322", "87654321");

        mvc.perform(get("/api/lectores/buscar").param("q", "pero"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].etiqueta").value("Pero Pérez"));
        mvc.perform(get("/api/lectores/buscar").param("q", "8765"))
                .andExpect(jsonPath("$[0].etiqueta").value("Maese Nicolás"));
        mvc.perform(get("/api/lectores/buscar"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void elFormularioDePrestamoConservaLaEleccionSiHayError() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);

        mvc.perform(post("/prestamos")
                        .param("libroIds", libro.getId().toString())
                        .param("fecha", RelojFijoConfig.HOY.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AMA-1 · Amadís de Gaula")))
                .andExpect(content().string(containsString("Escoged el lector")));
    }

    @Test
    void laPantallaDeConfiguracionMuestraYGuardaLasReglas() throws Exception {
        mvc.perform(get("/configuracion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Máximo de libros por lector")))
                .andExpect(content().string(containsString("VII.")))
                .andExpect(content().string(containsString("Descargar respaldo")))
                .andExpect(content().string(containsString("/reportes/morosos.csv")));

        mvc.perform(post("/configuracion")
                        .param("plazoDias", "10").param("multaPorDia", "2")
                        .param("maxLibrosPorLector", "3").param("bloquearMorosos", "true"))
                .andExpect(redirectedUrl("/configuracion"));

        org.assertj.core.api.Assertions.assertThat(prestamos.plazoDias()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(prestamos.multaPorDia()).isEqualTo(2);
    }

    @Test
    void laPantallaDeAuditoriaMuestraLosAsientosYFiltra() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        var p = prestamos.registrarPrestamo(libro.getId(), lector.getId(), RelojFijoConfig.HOY);
        prestamos.anular(p.getId());

        mvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("VI.")))
                .andExpect(content().string(containsString("Préstamo anulado")))
                .andExpect(content().string(containsString("Libro registrado")));
        // el detalle del alta del libro ("Montalvo, 1508") no debe salir al filtrar solo las anulaciones
        mvc.perform(get("/auditoria").param("accion", "PRESTAMO_ANULADO"))
                .andExpect(content().string(containsString("Pero Pérez (DNI 12345678)")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Montalvo, 1508"))));
        // un filtro inventado no rompe la pantalla
        mvc.perform(get("/auditoria").param("accion", "INVENTADA").param("pagina", "-5")).andExpect(status().isOk());
        mvc.perform(get("/auditoria").param("pagina", "999")).andExpect(status().isOk());
    }

    @Test
    void laConfiguracionRechazaValoresInvalidosConMensaje() throws Exception {
        mvc.perform(post("/configuracion")
                        .param("plazoDias", "0").param("multaPorDia", "1").param("maxLibrosPorLector", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("al menos 1 día")));
        org.assertj.core.api.Assertions.assertThat(prestamos.plazoDias()).isEqualTo(7);
    }

    @Test
    void elTopeDeLibrosSeMuestraEnElFormularioDePrestamo() throws Exception {
        mvc.perform(post("/configuracion").param("plazoDias", "7").param("multaPorDia", "1")
                .param("maxLibrosPorLector", "1"));
        Libro a = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Libro b = libros.registrar("TIR-1", "Tirante el Blanco", "Martorell", 1511);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");

        mvc.perform(post("/prestamos")
                        .param("libroIds", a.getId().toString(), b.getId().toString())
                        .param("lectorId", lector.getId().toString())
                        .param("fecha", RelojFijoConfig.HOY.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("el tope de la casa es de 1")));
    }

    @Test
    void lasPantallasDeEdicionResponden() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");

        mvc.perform(get("/libros/" + libro.getId() + "/editar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Amadís de Gaula")));
        mvc.perform(get("/lectores/" + lector.getId() + "/editar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("12345678")));

        mvc.perform(post("/libros/" + libro.getId() + "/editar")
                        .param("codigo", "ama-1").param("titulo", "Amadís corregido")
                        .param("autor", "Montalvo").param("anioImpresion", "1508"))
                .andExpect(redirectedUrl("/libros"));
        mvc.perform(post("/lectores/" + lector.getId() + "/editar")
                        .param("nombre", "Pero Pérez el cura").param("domicilio", "El Toboso")
                        .param("celular", "987654321").param("dni", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ocho guarismos")));
    }

    @Test
    void anularUnPrestamoPorLaPantallaYNoSeguirRedireccionesAjenas() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        var p = prestamos.registrarPrestamo(libro.getId(), lector.getId(), RelojFijoConfig.HOY);

        // un destino que no es una ruta interna se ignora: se vuelve a /prestamos
        mvc.perform(post("/prestamos/" + p.getId() + "/anular").param("volver", "//sitio-malo.example"))
                .andExpect(redirectedUrl("/prestamos"));
        org.assertj.core.api.Assertions.assertThat(prestamos.abiertos()).isEmpty();
    }

    @Test
    void borrarUnLibroConHistorialMuestraElErrorEnSuFicha() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");
        prestamos.registrarPrestamo(libro.getId(), lector.getId(), RelojFijoConfig.HOY);

        mvc.perform(post("/libros/" + libro.getId() + "/eliminar"))
                .andExpect(redirectedUrl("/libros/" + libro.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("historial")));
    }

    @Test
    void losDatosDeTipoIncorrectoDanMensajesEnEspanolYNoErroresTecnicos() throws Exception {
        mvc.perform(post("/libros").param("codigo", "X1").param("titulo", "T").param("autor", "A")
                        .param("anioImpresion", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("número")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Failed to convert"))));
        mvc.perform(post("/prestamos").param("libroIds", "1").param("lectorId", "1").param("fecha", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fecha válida")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Failed to convert"))));
        mvc.perform(post("/configuracion").param("plazoDias", "abc").param("multaPorDia", "1")
                        .param("maxLibrosPorLector", "0"))
                .andExpect(content().string(containsString("número entero")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Failed to convert"))));
    }

    @Test
    void elEndpointDeDniRechazaUnNumeroMalFormado() throws Exception {
        mvc.perform(get("/api/dni/1234"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("ocho guarismos")));
    }

    @Test
    void elEndpointDeDniAvisaSiFaltaElToken() throws Exception {
        // en el perfil de pruebas no hay DECOLECTA_TOKEN
        mvc.perform(get("/api/dni/46027897"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("DECOLECTA_TOKEN")));
    }

    @Test
    void asentarUnPrestamoValidoRedirige() throws Exception {
        Libro libro = libros.registrar("AMA-1", "Amadís de Gaula", "Montalvo", 1508);
        Lector lector = lectores.registrar("Pero Pérez", "El Toboso", "987654321", "12345678");

        mvc.perform(post("/prestamos")
                        .param("libroIds", libro.getId().toString())
                        .param("lectorId", lector.getId().toString())
                        .param("fecha", RelojFijoConfig.HOY.toString()))
                .andExpect(redirectedUrl("/prestamos"));
    }
}
