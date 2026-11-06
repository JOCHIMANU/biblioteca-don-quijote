package com.quijote.biblioteca.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.security.SecureRandom;
import java.util.LinkedHashMap;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private static final Logger log = LoggerFactory.getLogger(SeguridadConfig.class);

    /** Sin ambiguos (0/O, 1/l/I) para que la clave generada se pueda copiar a mano sin dudas. */
    private static final String ALFABETO = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * Qué se puede cargar en cada página. Nada de scripts ajenos ni en línea; solo las tipografías de
     * Google Fonts y las imágenes incrustadas (data:) que usa el diseño.
     */
    static final String POLITICA_DE_CONTENIDO = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src https://fonts.gstatic.com; "
            + "img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, LimiteDeIntentos limite) throws Exception {
        // al volver a la página que se pedía antes de entrar, sin colgarle «?continue» a la dirección
        HttpSessionRequestCache paginaPedida = new HttpSessionRequestCache();
        paginaPedida.setMatchingRequestParameterName(null);
        http
                .requestCache(c -> c.requestCache(paginaPedida))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/css/**", "/js/**", "/error",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(f -> f
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error"))
                .logout(l -> l
                        .logoutUrl("/salir")
                        .logoutSuccessUrl("/login?salio")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(alPedirSesion())
                        .accessDeniedHandler(alNegarElAcceso()))
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives(POLITICA_DE_CONTENIDO))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(f -> f.deny()))
                .addFilterBefore(new FiltroBloqueoDeAcceso(limite), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Qué pasa al pedir algo sin sesión: las consultas internas de la página (/api/**) reciben un 401 limpio;
     * todo lo demás, sea cual sea lo que el navegador diga aceptar, se lleva a la pantalla de entrada.
     */
    private AuthenticationEntryPoint alPedirSesion() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> segunCamino = new LinkedHashMap<>();
        segunCamino.put(peticion -> peticion.getRequestURI().startsWith("/api/"),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        DelegatingAuthenticationEntryPoint entrada = new DelegatingAuthenticationEntryPoint(segunCamino);
        entrada.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        return entrada;
    }

    /** Un formulario con la sesión caducada (token CSRF inválido) vuelve a la entrada; el resto, 403. */
    private AccessDeniedHandler alNegarElAcceso() {
        return (peticion, respuesta, excepcion) -> {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            boolean sinSesion = a == null || a instanceof AnonymousAuthenticationToken;
            if (excepcion instanceof CsrfException && sinSesion) {
                respuesta.sendRedirect(peticion.getContextPath() + "/login?expirada");
            } else {
                respuesta.sendError(HttpStatus.FORBIDDEN.value());
            }
        };
    }

    @Bean
    PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarios(SeguridadProps props, PasswordEncoder codificador) {
        String clave = props.getClave();
        if (clave == null || clave.isBlank()) {
            clave = claveAlAzar(16);
            log.warn("""

                    ==================================================================
                     No hay BIBLIOTECA_CLAVE configurada: se generó una clave para esta ejecución.
                       usuario: {}
                       clave:   {}
                     Para fijar la tuya, define la variable de entorno BIBLIOTECA_CLAVE.
                    ==================================================================""",
                    props.getUsuario(), clave);
        }
        return new InMemoryUserDetailsManager(User.withUsername(props.getUsuario())
                .password(codificador.encode(clave)).roles("ADMIN").build());
    }

    private static String claveAlAzar(int largo) {
        SecureRandom azar = new SecureRandom();
        StringBuilder sb = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) {
            sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
