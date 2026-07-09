package com.medicas.consultas.controlador.api;

import com.medicas.consultas.servicio.ML_CorreoAutomaticoService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/correos")
public class ML_CorreoAutomaticoApiController {

    private final ML_CorreoAutomaticoService ML_correoAutomaticoService;

    public ML_CorreoAutomaticoApiController(ML_CorreoAutomaticoService ML_correoAutomaticoService) {
        this.ML_correoAutomaticoService = ML_correoAutomaticoService;
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> ML_estadoCorreo() {
        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        ML_respuesta.put("ok", true);
        ML_respuesta.put("servicio", ML_correoAutomaticoService.ML_estadoServicioCorreo());
        return ResponseEntity.ok(ML_respuesta);
    }

    @PostMapping("/citas/{ML_idCita}/confirmacion")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public ResponseEntity<Map<String, Object>> ML_enviarConfirmacionCita(@PathVariable Integer ML_idCita) {
        return ResponseEntity.ok(ML_correoAutomaticoService.ML_enviarConfirmacionCita(ML_idCita));
    }

    @PostMapping("/pagos/{ML_idPago}/comprobante")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public ResponseEntity<Map<String, Object>> ML_enviarComprobantePago(@PathVariable Integer ML_idPago) {
        return ResponseEntity.ok(ML_correoAutomaticoService.ML_enviarComprobantePago(ML_idPago));
    }

    @PostMapping("/prueba")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> ML_enviarCorreoPrueba(@RequestParam("ML_correo") String ML_correo) {
        return ResponseEntity.ok(ML_correoAutomaticoService.ML_enviarCorreoPrueba(ML_correo));
    }
}
