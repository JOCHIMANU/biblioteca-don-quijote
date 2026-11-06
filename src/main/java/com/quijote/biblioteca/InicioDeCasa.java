package com.quijote.biblioteca;

import com.quijote.biblioteca.servicio.PrestamoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Pone al día, al arrancar, los datos guardados por versiones anteriores. */
@Component
@Order(0)
public class InicioDeCasa implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicioDeCasa.class);

    private final PrestamoService prestamos;

    public InicioDeCasa(PrestamoService prestamos) {
        this.prestamos = prestamos;
    }

    @Override
    public void run(ApplicationArguments args) {
        int n = prestamos.completarReglasHistoricas();
        if (n > 0) {
            log.info("{} préstamo(s) antiguo(s) heredan el plazo y la multa vigentes.", n);
        }
    }
}
