package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.servicio.ReporteService;
import com.quijote.biblioteca.servicio.RespaldoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Descargas: reportes en CSV y respaldo de la base de datos. */
@Controller
public class DescargasController {

    private final ReporteService reportes;
    private final RespaldoService respaldos;

    public DescargasController(ReporteService reportes, RespaldoService respaldos) {
        this.reportes = reportes;
        this.respaldos = respaldos;
    }

    @GetMapping("/reportes/{tipo}.csv")
    public ResponseEntity<byte[]> reporte(@PathVariable String tipo,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ReporteService.Reporte r = reportes.generar(tipo, desde, hasta);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(r.nombreArchivo()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(r.contenido());
    }

    @PostMapping("/respaldo")
    public ResponseEntity<byte[]> respaldo() {
        RespaldoService.Respaldo r = respaldos.crear();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(r.nombreArchivo()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(r.contenido());
    }
}
