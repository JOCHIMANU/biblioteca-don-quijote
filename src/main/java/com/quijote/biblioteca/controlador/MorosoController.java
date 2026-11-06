package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.servicio.PrestamoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MorosoController {

    private final PrestamoService prestamos;

    public MorosoController(PrestamoService prestamos) {
        this.prestamos = prestamos;
    }

    @GetMapping("/morosos")
    public String morosos(Model model) {
        model.addAttribute("morosos", prestamos.morosos());
        model.addAttribute("activo", 4);
        return "morosos";
    }
}
