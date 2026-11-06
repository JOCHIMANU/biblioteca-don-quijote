package com.quijote.biblioteca.servicio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.quijote.biblioteca.config.DecolectaProps;
import com.quijote.biblioteca.excepcion.ConsultaDniException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pregunta a Decolecta (RENIEC) el nombre que corresponde a un DNI. */
@Service
public class ReniecService {

    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");

    private final DecolectaProps props;
    private final RestClient cliente;

    public ReniecService(DecolectaProps props, RestClient.Builder constructor) {
        this.props = props;
        this.cliente = constructor.build(); // los tiempos de espera vienen de spring.http.client.*
    }

    /** Devuelve el nombre en orden natural: «Roxana Karina Delgado Huamani». */
    public String nombreDe(String dni) {
        if (dni == null || !DNI.matcher(dni.trim()).matches()) {
            throw new ConsultaDniException("El DNI ha de constar de ocho guarismos cabales.", true);
        }
        if (!props.tieneToken()) {
            throw new ConsultaDniException(
                    "Falta el token de Decolecta: ponedlo en la variable DECOLECTA_TOKEN y reiniciad la app.", false);
        }
        RespuestaDni r;
        try {
            r = cliente.get()
                    .uri(props.getUrl() + "?numero={dni}", dni.trim())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getToken().trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(RespuestaDni.class);
        } catch (HttpClientErrorException e) {
            int estado = e.getStatusCode().value();
            if (estado == 401 || estado == 403) {
                throw new ConsultaDniException("Decolecta no aceptó el token: revisad que sea el correcto.", false);
            }
            if (estado == 429) {
                throw new ConsultaDniException("Decolecta pide un respiro: se agotó el cupo de consultas.", false);
            }
            throw new ConsultaDniException("Ese DNI no consta en el registro de Decolecta.", true);
        } catch (RestClientException e) {
            throw new ConsultaDniException("No se pudo consultar a Decolecta; escribid el nombre a mano.", false);
        }
        if (r == null || r.firstName == null || r.firstName.isBlank()) {
            throw new ConsultaDniException("Ese DNI no consta en el registro de Decolecta.", true);
        }
        return aTitulo(String.join(" ", r.firstName, nvl(r.firstLastName), nvl(r.secondLastName)));
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /** «ROXANA KARINA  DELGADO» → «Roxana Karina Delgado» (respeta partículas como «de», «del», «la»). */
    static String aTitulo(String texto) {
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.trim().toLowerCase(new Locale("es", "PE")).split("\\s+")) {
            if (palabra.isEmpty()) {
                continue;
            }
            boolean particula = sb.length() > 0 && (palabra.equals("de") || palabra.equals("del")
                    || palabra.equals("la") || palabra.equals("las") || palabra.equals("los") || palabra.equals("y"));
            sb.append(sb.length() > 0 ? " " : "")
                    .append(particula ? palabra : Character.toUpperCase(palabra.charAt(0)) + palabra.substring(1));
        }
        return sb.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaDni(@JsonProperty("first_name") String firstName,
                        @JsonProperty("first_last_name") String firstLastName,
                        @JsonProperty("second_last_name") String secondLastName) {
    }
}
