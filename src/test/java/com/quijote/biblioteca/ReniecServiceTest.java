package com.quijote.biblioteca;

import com.quijote.biblioteca.config.DecolectaProps;
import com.quijote.biblioteca.excepcion.ConsultaDniException;
import com.quijote.biblioteca.servicio.ReniecService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Prueba la consulta de DNI contra un servidor simulado: no gasta cupo ni necesita el token real. */
class ReniecServiceTest {

    private static final String URL = "https://api.decolecta.test/v1/reniec/dni";

    private MockRestServiceServer servidor;
    private ReniecService servicio;
    private DecolectaProps props;

    @BeforeEach
    void preparar() {
        props = new DecolectaProps();
        props.setUrl(URL);
        props.setToken("token-de-prueba");
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        servicio = new ReniecService(props, constructor);
    }

    @Test
    void devuelveElNombreEnOrdenNaturalYCapitalizado() {
        servidor.expect(requestTo(URL + "?numero=46027897"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andRespond(withSuccess("""
                        {"first_name":"ROXANA KARINA","first_last_name":"DELGADO","second_last_name":"HUAMANI",
                         "full_name":"DELGADO HUAMANI ROXANA KARINA","document_number":"46027897"}""",
                        MediaType.APPLICATION_JSON));

        assertThat(servicio.nombreDe("46027897")).isEqualTo("Roxana Karina Delgado Huamani");
        servidor.verify();
    }

    @Test
    void respetaLasParticulasDeLosApellidos() {
        servidor.expect(requestTo(URL + "?numero=12345678"))
                .andRespond(withSuccess("""
                        {"first_name":"JUAN","first_last_name":"DE LA CRUZ","second_last_name":"DEL RIO"}""",
                        MediaType.APPLICATION_JSON));

        assertThat(servicio.nombreDe("12345678")).isEqualTo("Juan de la Cruz del Rio");
    }

    @Test
    void unDniMalFormadoNoLlamaAlServicioExterno() {
        assertThatThrownBy(() -> servicio.nombreDe("1234"))
                .isInstanceOfSatisfying(ConsultaDniException.class, e -> assertThat(e.esNoEncontrado()).isTrue());
        assertThatThrownBy(() -> servicio.nombreDe("1234567a")).isInstanceOf(ConsultaDniException.class);
        servidor.verify(); // no se esperaba ninguna petición
    }

    @Test
    void sinTokenAvisaDeQueFaltaConfigurarlo() {
        props.setToken("");
        assertThatThrownBy(() -> servicio.nombreDe("46027897"))
                .isInstanceOfSatisfying(ConsultaDniException.class, e -> {
                    assertThat(e.esNoEncontrado()).isFalse();
                    assertThat(e.getMessage()).contains("DECOLECTA_TOKEN");
                });
    }

    @Test
    void unDniQueNoExisteSeReportaComoNoEncontrado() {
        servidor.expect(requestTo(URL + "?numero=00000000"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Invalid request\"}"));

        assertThatThrownBy(() -> servicio.nombreDe("00000000"))
                .isInstanceOfSatisfying(ConsultaDniException.class, e -> assertThat(e.esNoEncontrado()).isTrue());
    }

    @Test
    void unTokenRechazadoNoSeConfundeConUnDniInexistente() {
        servidor.expect(requestTo(URL + "?numero=46027897")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> servicio.nombreDe("46027897"))
                .isInstanceOfSatisfying(ConsultaDniException.class, e -> {
                    assertThat(e.esNoEncontrado()).isFalse();
                    assertThat(e.getMessage()).contains("token");
                });
    }

    @Test
    void siDecolectaFallaSePideEscribirElNombreAMano() {
        servidor.expect(requestTo(URL + "?numero=46027897")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> servicio.nombreDe("46027897"))
                .isInstanceOfSatisfying(ConsultaDniException.class, e -> {
                    assertThat(e.esNoEncontrado()).isFalse();
                    assertThat(e.getMessage()).contains("a mano");
                });
    }
}
