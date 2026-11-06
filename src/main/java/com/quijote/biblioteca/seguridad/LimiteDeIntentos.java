package com.quijote.biblioteca.seguridad;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno básico contra quien adivina la clave a fuerza bruta: tras {@code maxIntentos} fallos desde una
 * misma dirección, no se admiten más intentos hasta que pasa la ventana (contada desde el primer fallo).
 * Vive en memoria: al reiniciar la app se olvida, lo cual basta para este caso.
 */
@Component
public class LimiteDeIntentos {

    private record Anotacion(int fallos, Instant desde) {
    }

    private static final int MAX_DIRECCIONES = 5_000;

    private final int maxIntentos;
    private final Duration ventana;
    private final Clock reloj;
    private final ConcurrentHashMap<String, Anotacion> porDireccion = new ConcurrentHashMap<>();

    public LimiteDeIntentos(SeguridadProps props, Clock reloj) {
        this.maxIntentos = props.getMaxIntentos();
        this.ventana = Duration.ofMinutes(props.getMinutosBloqueo());
        this.reloj = reloj;
    }

    public boolean bloqueada(String direccion) {
        Anotacion a = porDireccion.get(direccion);
        if (a == null) {
            return false;
        }
        if (vencida(a)) {
            porDireccion.remove(direccion, a);
            return false;
        }
        return a.fallos() >= maxIntentos;
    }

    public void fallo(String direccion) {
        Instant ahora = reloj.instant();
        porDireccion.merge(direccion, new Anotacion(1, ahora),
                (anterior, nueva) -> vencida(anterior) ? nueva : new Anotacion(anterior.fallos() + 1, anterior.desde()));
        if (porDireccion.size() > MAX_DIRECCIONES) {
            porDireccion.values().removeIf(this::vencida); // que un atacante no llene la memoria
        }
    }

    public void exito(String direccion) {
        porDireccion.remove(direccion);
    }

    private boolean vencida(Anotacion a) {
        return reloj.instant().isAfter(a.desde().plus(ventana));
    }
}
