package com.quijote.biblioteca.servicio;

import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.modelo.AccionAuditada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Copia de seguridad de la base de datos. Con H2 (en tu equipo) baja un .zip con el archivo de datos,
 * consistente aunque la app esté en uso. Con PostgreSQL (en la nube) la copia la hace el proveedor de la base.
 */
@Service
public class RespaldoService {

    public record Respaldo(String nombreArchivo, byte[] contenido) {
    }

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    private final DataSource datos;
    private final AuditoriaService auditoria;
    private final Clock reloj;

    public RespaldoService(DataSource datos, AuditoriaService auditoria, Clock reloj) {
        this.datos = datos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Nombre del motor de base de datos en uso (H2, PostgreSQL…). */
    public String nombreBaseDeDatos() {
        try (Connection c = datos.getConnection()) {
            return c.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            return "desconocida";
        }
    }

    /** El respaldo en un clic solo existe para H2; PostgreSQL se respalda desde el panel de su proveedor. */
    public boolean disponible() {
        return nombreBaseDeDatos().toLowerCase().contains("h2");
    }

    @Transactional
    public Respaldo crear() {
        if (!disponible()) {
            throw new BibliotecaException("Esta biblioteca guarda sus datos en " + nombreBaseDeDatos()
                    + ", en la nube: la copia de seguridad se hace desde el panel del proveedor de la base de datos.");
        }
        Path temporal = null;
        try {
            temporal = Files.createTempFile("respaldo-biblioteca-", ".zip");
            Files.delete(temporal); // H2 crea el archivo; no debe existir ya
            try (Connection c = datos.getConnection(); Statement s = c.createStatement()) {
                s.execute("BACKUP TO '" + temporal.toString().replace('\\', '/').replace("'", "''") + "'");
            }
            byte[] zip = Files.readAllBytes(temporal);
            String nombre = "biblioteca-respaldo-" + LocalDateTime.now(reloj).format(SELLO) + ".zip";
            auditoria.registrar(AccionAuditada.RESPALDO, nombre + " (" + zip.length + " bytes)");
            return new Respaldo(nombre, zip);
        } catch (SQLException | IOException e) {
            throw new BibliotecaException("No se pudo preparar el respaldo: " + e.getMessage());
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignorada) {
                    // un temporal que no se borra no estorba al usuario
                }
            }
        }
    }
}
