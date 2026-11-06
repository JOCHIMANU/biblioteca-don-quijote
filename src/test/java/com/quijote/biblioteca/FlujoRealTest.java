package com.quijote.biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recorrido HTTP real (servidor y cookies de verdad, no simulados): MockMvc no confirma la respuesta como lo hace
 * Tomcat, y un error de ese tipo (crear la sesión con la página ya enviada) solo aparece aquí.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"decolecta.token=", "biblioteca.seguridad.clave=clave-de-prueba-123"})
@ActiveProfiles("test")
class FlujoRealTest {

    private static final Pattern TOKEN = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    @Value("${local.server.port}")
    int puerto;

    private final HttpClient cliente = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }

    private HttpResponse<String> get(String ruta) throws Exception {
        return cliente.send(HttpRequest.newBuilder(uri(ruta)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String ruta, String cuerpo) throws Exception {
        return cliente.send(HttpRequest.newBuilder(uri(ruta))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String campo(String nombre, String valor) {
        return URLEncoder.encode(nombre, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private String tokenDe(HttpResponse<String> pagina) {
        Matcher m = TOKEN.matcher(pagina.body());
        assertThat(m.find()).as("la página trae un token CSRF").isTrue();
        return m.group(1);
    }

    @Test
    void laPantallaDeEntradaSeDibujaYCreaLaSesionConUnServidorReal() throws Exception {
        HttpResponse<String> entrada = get("/login");

        assertThat(entrada.statusCode()).isEqualTo(200);
        assertThat(entrada.body()).contains("Entrada a la biblioteca");
        assertThat(entrada.headers().allValues("Set-Cookie")).anyMatch(c -> c.startsWith("JSESSIONID="));
        assertThat(entrada.headers().firstValue("Set-Cookie").orElse("")).containsIgnoringCase("HttpOnly")
                .containsIgnoringCase("SameSite=Lax");
        assertThat(tokenDe(entrada)).isNotBlank();
    }

    @Test
    void entrarNavegarYSalirDeVerdad() throws Exception {
        // sin sesión, la portada manda a la entrada
        HttpResponse<String> sinSesion = get("/");
        assertThat(sinSesion.statusCode()).isEqualTo(302);
        assertThat(sinSesion.headers().firstValue("Location").orElse("")).endsWith("/login");

        // clave mala
        String token = tokenDe(get("/login"));
        HttpResponse<String> mala = post("/login", campo("username", "admin") + "&" + campo("password", "no-es")
                + "&" + campo("_csrf", token));
        assertThat(mala.headers().firstValue("Location").orElse("")).endsWith("/login?error");

        // clave buena
        token = tokenDe(get("/login"));
        HttpResponse<String> buena = post("/login", campo("username", "admin") + "&" + campo("password", "clave-de-prueba-123")
                + "&" + campo("_csrf", token));
        assertThat(buena.statusCode()).isEqualTo(302);

        assertThat(buena.headers().firstValue("Location").orElse("")).doesNotContain("continue");
        HttpResponse<String> portada = get("/");
        assertThat(portada.statusCode()).isEqualTo(200);
        // los estáticos llevan la huella del contenido y se guardan un año: se pueden cambiar sin romper a nadie
        Matcher js = Pattern.compile("src=\"(/js/biblioteca-[0-9a-f]+\\.js)\"").matcher(portada.body());
        assertThat(js.find()).as("el script va con huella en el nombre").isTrue();
        HttpResponse<String> estatico = get(js.group(1));
        assertThat(estatico.statusCode()).isEqualTo(200);
        assertThat(estatico.headers().firstValue("Cache-Control").orElse("")).contains("max-age=31536000");
        assertThat(portada.body()).contains("Frontispicio").contains("usuario-nombre").contains(">admin<");
        assertThat(get("/libros").statusCode()).isEqualTo(200);
        assertThat(get("/auditoria").statusCode()).isEqualTo(200);

        // un formulario con el token de la propia página funciona; sin token, no
        String tokenDeLaPagina = tokenDe(get("/libros"));
        HttpResponse<String> sinToken = post("/libros", campo("codigo", "X-1") + "&" + campo("titulo", "T")
                + "&" + campo("autor", "A") + "&" + campo("anioImpresion", "1508"));
        assertThat(sinToken.statusCode()).isEqualTo(403);
        HttpResponse<String> conToken = post("/libros", campo("codigo", "X-1") + "&" + campo("titulo", "Libro de prueba")
                + "&" + campo("autor", "A") + "&" + campo("anioImpresion", "1508") + "&" + campo("_csrf", tokenDeLaPagina));
        assertThat(conToken.statusCode()).isEqualTo(302);
        assertThat(get("/libros").body()).contains("Libro de prueba");

        // salir cierra la sesión
        HttpResponse<String> salida = post("/salir", campo("_csrf", tokenDe(get("/libros"))));
        assertThat(salida.headers().firstValue("Location").orElse("")).endsWith("/login?salio");
        assertThat(get("/libros").statusCode()).isEqualTo(302);
    }
}
