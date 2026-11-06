package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.servicio.AvisoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PalomaController {

    private final AvisoService avisos;

    public PalomaController(AvisoService avisos) {
        this.avisos = avisos;
    }

    @GetMapping("/palomas")
    public String panel(Model model) {
        avisos.generarAvisos(); // también al abrir el panel
        model.addAttribute("pendientes", avisos.pendientes());
        model.addAttribute("reclamados", avisos.reclamadosRecientes());
        model.addAttribute("avisosPendientes", avisos.contarPendientes());
        model.addAttribute("activo", 5);
        return "palomas";
    }

    @PostMapping("/palomas/{id}/reclamar")
    public String reclamar(@PathVariable Long id, RedirectAttributes flash) {
        avisos.reclamar(id);
        flash.addFlashAttribute("ok", "Paloma despachada: el escudero Sancho parte a reclamar el libro.");
        return "redirect:/palomas";
    }
}
