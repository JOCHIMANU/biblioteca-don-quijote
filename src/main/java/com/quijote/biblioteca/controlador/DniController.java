package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.excepcion.ConsultaDniException;
import com.quijote.biblioteca.servicio.ReniecService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** El formulario de lectores llama aquí; el token de Decolecta se queda en el servidor. */
@RestController
public class DniController {

    private final ReniecService reniec;

    public DniController(ReniecService reniec) {
        this.reniec = reniec;
    }

    @GetMapping("/api/dni/{dni}")
    public ResponseEntity<Map<String, String>> consultar(@PathVariable String dni) {
        try {
            return ResponseEntity.ok(Map.of("nombre", reniec.nombreDe(dni)));
        } catch (ConsultaDniException e) {
            HttpStatus estado = e.esNoEncontrado() ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(estado).body(Map.of("error", e.getMessage()));
        }
    }
}
