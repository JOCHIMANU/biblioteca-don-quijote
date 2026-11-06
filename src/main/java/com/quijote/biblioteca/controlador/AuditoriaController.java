package com.quijote.biblioteca.controlador;

import com.quijote.biblioteca.modelo.AccionAuditada;
import com.quijote.biblioteca.modelo.RegistroAuditoria;
import com.quijote.biblioteca.servicio.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditoriaController {

    private final AuditoriaService auditoria;

    public AuditoriaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping("/auditoria")
    public String libro(@RequestParam(required = false) String accion,
                        @RequestParam(defaultValue = "0") int pagina, Model model) {
        AccionAuditada filtro = null;
        if (accion != null && !accion.isBlank()) {
            try {
                filtro = AccionAuditada.valueOf(accion);
            } catch (IllegalArgumentException e) {
                // un filtro desconocido se ignora y se muestran todas
            }
        }
        Page<RegistroAuditoria> pagina1 = auditoria.pagina(filtro, pagina);
        model.addAttribute("pagina", pagina1);
        model.addAttribute("acciones", AccionAuditada.values());
        model.addAttribute("filtro", filtro);
        model.addAttribute("activo", 6);
        return "auditoria";
    }
}
