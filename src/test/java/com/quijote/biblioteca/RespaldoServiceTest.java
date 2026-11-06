package com.quijote.biblioteca;

import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.servicio.AuditoriaService;
import com.quijote.biblioteca.servicio.RespaldoService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Prueba el respaldo contra una base H2 de archivo de verdad (el respaldo no existe para bases en memoria). */
class RespaldoServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void elRespaldoEsUnZipConLaBaseYSeAsientaEnLaAuditoria() throws Exception {
        JdbcDataSource datos = new JdbcDataSource();
        datos.setURL("jdbc:h2:file:" + carpeta.resolve("origen").toString().replace('\\', '/'));
        datos.setUser("sa");
        try (Connection c = datos.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE libro (id INT PRIMARY KEY, titulo VARCHAR(100))");
            s.execute("INSERT INTO libro VALUES (1, 'Amadís de Gaula')");
        }
        AuditoriaService auditoria = mock(AuditoriaService.class);
        Clock reloj = Clock.fixed(RelojFijoConfig.HOY.atTime(15, 42).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());

        RespaldoService.Respaldo r = new RespaldoService(datos, auditoria, reloj).crear();

        assertThat(r.nombreArchivo()).isEqualTo("biblioteca-respaldo-2026-10-30_1542.zip");
        assertThat(r.contenido()).isNotEmpty();
        List<String> entradas = new ArrayList<>();
        Path restaurada = carpeta.resolve("restaurada");
        Files.createDirectories(restaurada);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(r.contenido()))) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                entradas.add(e.getName());
                Files.write(restaurada.resolve("copia.mv.db"), zip.readAllBytes());
            }
        }
        assertThat(entradas).singleElement().asString().endsWith(".mv.db");
        verify(auditoria).registrar(org.mockito.ArgumentMatchers.eq(AccionAuditada.RESPALDO),
                org.mockito.ArgumentMatchers.contains("biblioteca-respaldo-2026-10-30_1542.zip"));

        // restaurar de verdad: abrir la copia y encontrar los datos
        JdbcDataSource copia = new JdbcDataSource();
        copia.setURL("jdbc:h2:file:" + restaurada.resolve("copia").toString().replace('\\', '/'));
        copia.setUser("sa");
        try (Connection c = copia.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT titulo FROM libro WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("Amadís de Gaula");
        }
    }
}
