package com.quijote.biblioteca;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Entrada con usuario y contraseña: sin sesión no se ve nada, y los formularios llevan token CSRF. */
@SpringBootTest(properties = {"decolecta.token=", "biblioteca.seguridad.clave=clave-de-prueba-123",
        "biblioteca.seguridad.max-intentos=3"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojFijoConfig.class)
@Transactional
class SeguridadTest {

    private static final String CLAVE = "clave-de-prueba-123";

    @Autowired MockMvc mvc;

    @Test
    void sinSesionTodaPantallaVuelveALaEntrada() throws Exception {
        for (String ruta : new String[]{"/", "/libros", "/lectores", "/prestamos", "/prestamos/nuevo", "/morosos",
                "/palomas", "/auditoria", "/configuracion", "/reportes/libros.csv", "/libros/1", "/lectores/1/editar"}) {
            mvc.perform(get(ruta)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }
    }

    @Test
    void lasAccionesQueModificanOBajanDatosTampocoFuncionanSinSesion() throws Exception {
        for (String ruta : new String[]{"/libros", "/lectores", "/prestamos", "/configuracion", "/respaldo",
                "/prestamos/1/anular", "/libros/1/eliminar", "/palomas/1/reclamar"}) {
            mvc.perform(post(ruta).with(csrf())).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }
    }

    @Test
    void lasConsultasInternasSinSesionRecibenUn401LimpioYNoLaPantallaDeEntrada() throws Exception {
        mvc.perform(get("/api/dni/46027897")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/libros/disponibles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/lectores/buscar")).andExpect(status().isUnauthorized());
    }

    @Test
    void laEntradaLosEstaticosYLaSaludSonPublicos() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Entrada a la biblioteca")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Anaquel")))); // sin índice sin sesión
        mvc.perform(get("/css/estilo.css")).andExpect(status().isOk());
        mvc.perform(get("/js/biblioteca.js")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        // y lo demás de actuator no se expone
        mvc.perform(get("/actuator/env")).andExpect(status().is3xxRedirection());
    }

    @Test
    void conLaClaveCorrectaSeEntraYSeLlegaAlFrontispicio() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password(CLAVE))
                .andExpect(authenticated().withUsername("admin"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void conUnaClaveIncorrectaNoSeEntra() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("otra-cosa"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin("/login").user("intruso").password(CLAVE))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void laPantallaDeEntradaExplicaLosMotivos() throws Exception {
        mvc.perform(get("/login").param("error", "")).andExpect(content().string(containsString("incorrectos")));
        mvc.perform(get("/login").param("bloqueado", "")).andExpect(content().string(containsString("Demasiados intentos")));
        mvc.perform(get("/login").param("expirada", "")).andExpect(content().string(containsString("sesión caducó")));
        mvc.perform(get("/login").param("salio", "")).andExpect(content().string(containsString("Hasta la vista")));
        // sin parámetros no hay ningún aviso
        mvc.perform(get("/login")).andExpect(content().string(org.hamcrest.Matchers.not(containsString("incorrectos"))));
    }

    @Test
    void trasVariosFallosSeguidosLaDireccionQuedaBloqueadaAunqueAcierte() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(entrar("admin", "mal-" + i, "10.9.9.1")).andExpect(redirectedUrl("/login?error"));
        }
        // la cuarta, con la clave buena, ya no se atiende
        mvc.perform(entrar("admin", CLAVE, "10.9.9.1"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?bloqueado"));
        // otra dirección no se ve afectada
        mvc.perform(entrar("admin", CLAVE, "10.9.9.2")).andExpect(authenticated());
    }

    @Test
    void unAciertoAntesDelLimiteReiniciaLaCuenta() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(entrar("admin", "mal", "10.9.9.3"));
        }
        mvc.perform(entrar("admin", CLAVE, "10.9.9.3")).andExpect(authenticated());
        for (int i = 0; i < 2; i++) {
            mvc.perform(entrar("admin", "mal", "10.9.9.3")).andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(entrar("admin", CLAVE, "10.9.9.3")).andExpect(authenticated());
    }

    @Test
    @WithMockUser
    void unFormularioSinTokenCsrfSeRechazaAunConSesion() throws Exception {
        mvc.perform(post("/libros").param("codigo", "X-1").param("titulo", "T").param("autor", "A")
                        .param("anioImpresion", "1508"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/libros").with(csrf()).param("codigo", "X-1").param("titulo", "T").param("autor", "A")
                        .param("anioImpresion", "1508"))
                .andExpect(redirectedUrl("/libros"));
    }

    @Test
    void unFormularioSinTokenYSinSesionVuelveALaEntradaDiciendoQueCaduco() throws Exception {
        mvc.perform(post("/libros").param("codigo", "X-1"))
                .andExpect(redirectedUrl("/login?expirada"));
    }

    @Test
    @WithMockUser
    void salirCierraLaSesion() throws Exception {
        mvc.perform(post("/salir").with(csrf()))
                .andExpect(redirectedUrl("/login?salio"))
                .andExpect(unauthenticated());
    }

    @Test
    void lasCabecerasDeSeguridadViajanEnCadaRespuesta() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @WithMockUser(username = "docente")
    void laAuditoriaAnotaQuienHizoCadaCosa() throws Exception {
        mvc.perform(post("/libros").with(csrf()).param("codigo", "AMA-1").param("titulo", "Amadís de Gaula")
                        .param("autor", "Montalvo").param("anioImpresion", "1508"))
                .andExpect(redirectedUrl("/libros"));

        mvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Amadís de Gaula")))
                .andExpect(content().string(containsString("docente")));
    }

    @Test
    @WithMockUser
    void todasLasPaginasMuestranElUsuarioYElBotonSalir() throws Exception {
        mvc.perform(get("/libros"))
                .andExpect(content().string(containsString("usuario-nombre")))
                .andExpect(content().string(containsString("/salir")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    /** Un intento de entrada hecho desde la dirección {@code ip} (como el formulario de la pantalla de entrada). */
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder entrar(
            String usuario, String clave, String ip) {
        return post("/login").with(csrf()).param("username", usuario).param("password", clave)
                .with(peticion -> {
                    peticion.setRemoteAddr(ip);
                    return peticion;
                });
    }
}
