package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.config.DecolectaProps;
import com.quijote.biblioteca.excepcion.BibliotecaException;
import com.quijote.biblioteca.servicio.ConfiguracionService;
import com.quijote.biblioteca.servicio.RespaldoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConfiguracionController {

    private final ConfiguracionService config;
    private final DecolectaProps decolecta;
    private final RespaldoService respaldos;
    private final String cronPalomas;

    public ConfiguracionController(ConfiguracionService config, DecolectaProps decolecta, RespaldoService respaldos,
                                   @Value("${biblioteca.cron-palomas:0 0 8 * * *}") String cronPalomas) {
        this.config = config;
        this.decolecta = decolecta;
        this.respaldos = respaldos;
        this.cronPalomas = cronPalomas;
    }

    @GetMapping("/configuracion")
    public String ver(Model model) {
        ConfiguracionForm form = new ConfiguracionForm();
        form.setPlazoDias(config.plazoDias());
        form.setMultaPorDia(config.multaPorDia());
        form.setMaxLibrosPorLector(config.maxLibrosPorLector());
        form.setBloquearMorosos(config.bloquearMorosos());
        model.addAttribute("form", form);
        return vista(model);
    }

    @PostMapping("/configuracion")
    public String guardar(@Valid @ModelAttribute("form") ConfiguracionForm form, BindingResult resultado,
                          Model model, RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                config.guardar(form.getPlazoDias(), form.getMultaPorDia(), form.getMaxLibrosPorLector(),
                        form.isBloquearMorosos());
                flash.addFlashAttribute("ok", "Configuración guardada. Rige desde ahora para los préstamos nuevos.");
                return "redirect:/configuracion";
            } catch (BibliotecaException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return vista(model);
    }

    private String vista(Model model) {
        model.addAttribute("tokenDecolecta", decolecta.tieneToken());
        model.addAttribute("cronPalomas", cronPalomas);
        model.addAttribute("baseDeDatos", respaldos.nombreBaseDeDatos());
        model.addAttribute("respaldoDisponible", respaldos.disponible());
        model.addAttribute("activo", 7);
        return "configuracion";
    }
}
